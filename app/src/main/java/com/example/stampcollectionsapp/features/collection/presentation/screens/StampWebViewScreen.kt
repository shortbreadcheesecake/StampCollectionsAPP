package com.example.stampcollectionsapp.features.collection.presentation.screens

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

data class StampSearchResult(
    val id: String,
    val country: String,
    val year: String,
    val denomination: String?,
    val description: String?,
    val imageUrl: String?,
    val price: String?,
    val detailUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampWebViewScreen(
    url: String = "https://colnect.com/ru/stamps",
    onBack: () -> Unit,
    onStampSelected: (stampData: Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val loginPreferences = remember {
        context.getSharedPreferences("colnect_auth", android.content.Context.MODE_PRIVATE)
    }
    var hasLoggedInBefore by remember {
        mutableStateOf(loginPreferences.getBoolean("colnect_logged_in", false))
    }
    var isLoading by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(hasLoggedInBefore) }
    var showLoginScreen by remember { mutableStateOf(!hasLoggedInBefore) }
    var currentStampDetailUrl by remember { mutableStateOf<String?>(null) }
    var checkLoginAttempts by remember { mutableStateOf(0) }
    
    // JavaScript для проверки входа в Colnect
    val checkLoginJavaScript = """
        (function() {
            try {
                // Проверяем различные индикаторы входа в Colnect
                // 1. Наличие ссылок на профиль коллекционера
                var collectorLinks = document.querySelectorAll('a[href*="/collectors/"]');
                if (collectorLinks.length > 0) {
                    // Проверяем, что это не просто ссылка на каталог коллекционеров
                    for (var i = 0; i < collectorLinks.length; i++) {
                        var href = collectorLinks[i].href;
                        var text = collectorLinks[i].textContent.trim();
                        // Если ссылка содержит имя пользователя или "мой профиль", значит пользователь вошел
                        if (href.match(/\/collectors\/[^\/]+\/?$/) || text.toLowerCase().match(/профиль|мой|my|profile/i)) {
                            return true;
                        }
                    }
                }
                
                // 2. Наличие элементов меню пользователя
                var userMenu = document.querySelector('.user_menu, .user-info, [class*="user-menu"], [id*="user"]');
                if (userMenu) {
                    return true;
                }
                
                // 3. Проверяем наличие кнопки выхода
                var logoutLinks = document.querySelectorAll('a');
                for (var i = 0; i < logoutLinks.length; i++) {
                    var text = logoutLinks[i].textContent.trim().toLowerCase();
                    if (text === 'выход' || text === 'logout' || text === 'выйти' || logoutLinks[i].href.includes('logout')) {
                        return true;
                    }
                }
                
                // 4. Проверяем, что мы не на странице входа
                var isLoginPage = window.location.href.includes('/login') || 
                                  window.location.href.includes('/signin') ||
                                  document.querySelector('form[action*="login"]') !== null ||
                                  document.querySelector('#login_form') !== null ||
                                  document.querySelector('.login_form') !== null;
                
                // 5. Проверяем наличие элементов, характерных для авторизованных пользователей
                var authenticatedElements = document.querySelectorAll('[class*="collector"], [id*="collector"]');
                if (authenticatedElements.length > 2 && !isLoginPage) {
                    // Если есть несколько элементов, связанных с коллекционерами, и мы не на странице входа
                    return true;
                }
                
                // Если мы на главной странице марок и нет формы входа, считаем что пользователь может быть вошел
                if (window.location.href.includes('/ru/stamps') && !isLoginPage) {
                    // Дополнительная проверка - ищем элементы навигации, которые показываются только авторизованным
                    var navElements = document.querySelectorAll('nav a, .navigation a');
                    var hasCollectorNav = false;
                    for (var i = 0; i < navElements.length; i++) {
                        if (navElements[i].href.includes('/collectors/') && !navElements[i].href.includes('/collectors/list')) {
                            hasCollectorNav = true;
                            break;
                        }
                    }
                    if (hasCollectorNav) {
                        return true;
                    }
                }
                
                return false;
            } catch(e) {
                console.error('Check login error: ' + e.message);
                return false;
            }
        })();
    """.trimIndent()
    
    // JavaScript для парсинга страницы поиска Colnect.com
    val parseSearchResultsJavaScript = """
        (function() {
            var results = [];
            try {
                // Парсинг результатов поиска с Colnect.com
                // Ищем строки таблицы с марками
                var table = document.querySelector('table.list_table, table[class*="list"], table.items_table');
                if (!table) {
                    // Пробуем найти любую таблицу
                    table = document.querySelector('table');
                }
                
                if (table) {
                    var rows = table.querySelectorAll('tbody tr, tr');
                    rows.forEach(function(row, index) {
                        // Пропускаем заголовки
                        if (row.querySelector('th') || row.classList.contains('header')) {
                            return;
                        }
                        
                        var country = '';
                        var year = '';
                        var denomination = '';
                        var description = '';
                        var imageUrl = '';
                        var detailUrl = '';
                        
                        // Ищем все ссылки на марки
                        var stampLinks = row.querySelectorAll('a[href*="/stamps/stamp/"]');
                        if (stampLinks.length > 0) {
                            var mainLink = stampLinks[0];
                            detailUrl = mainLink.href;
                            description = mainLink.textContent.trim();
                            
                            // Если описание пустое, берем из title или alt
                            if (!description) {
                                description = mainLink.getAttribute('title') || mainLink.getAttribute('alt') || '';
                            }
                        }
                        
                        // Ищем изображение
                        var imgEl = row.querySelector('img');
                        if (imgEl) {
                            imageUrl = imgEl.src || imgEl.getAttribute('data-src') || imgEl.getAttribute('data-lazy-src') || '';
                            if (imageUrl && !imageUrl.startsWith('http')) {
                                if (imageUrl.startsWith('//')) {
                                    imageUrl = 'https:' + imageUrl;
                                } else {
                                    imageUrl = 'https://colnect.com' + (imageUrl.startsWith('/') ? imageUrl : '/' + imageUrl);
                                }
                            }
                            
                            // Если нет описания, пытаемся взять из alt изображения
                            if (!description && imgEl.alt) {
                                description = imgEl.alt;
                            }
                        }
                        
                        // Извлекаем данные из ячеек
                        var cells = row.querySelectorAll('td');
                        cells.forEach(function(cell) {
                            var text = cell.textContent.trim();
                            
                            // Ищем ссылку на страну
                            var countryLink = cell.querySelector('a[href*="/stamps/list/country/"], a[href*="/countries/"]');
                            if (countryLink && !country) {
                                country = countryLink.textContent.trim();
                            }
                            
                            // Ищем год (4 цифры)
                            if (!year) {
                                var yearMatch = text.match(/\b(19|20)\d{2}\b/);
                                if (yearMatch) {
                                    year = yearMatch[0];
                                }
                            }
                            
                            // Ищем номинал
                            if (!denomination) {
                                // Пробуем разные паттерны для номинала
                                var denomPatterns = [
                                    /\d+[\.,]\d+\s*[А-ЯЁа-яёA-Za-z]+/,
                                    /\d+\s*[А-ЯЁа-яёA-Za-z]+/,
                                    /[А-ЯЁа-яё]+\s*\d+/
                                ];
                                
                                for (var i = 0; i < denomPatterns.length; i++) {
                                    var match = text.match(denomPatterns[i]);
                                    if (match && !yearMatch) {
                                        denomination = match[0];
                                        break;
                                    }
                                }
                            }
                        });
                        
                        // Если есть хотя бы ссылка или изображение, добавляем результат
                        if (detailUrl || imageUrl || description) {
                            results.push({
                                id: 'stamp_' + index,
                                country: country || '',
                                year: year || '',
                                denomination: denomination || '',
                                description: description || '',
                                imageUrl: imageUrl || '',
                                price: '',
                                detailUrl: detailUrl || ''
                            });
                        }
                    });
                }
            } catch(e) {
                console.error('Parse error: ' + e.message);
            }
            return JSON.stringify(results);
        })();
    """.trimIndent()
    
    // JavaScript для парсинга детальной страницы марки Colnect.com
    val parseStampDetailJavaScript = """
        (function() {
            var result = {};
            try {
                // Парсинг детальной страницы марки с Colnect.com
                
                // Извлечение названия из заголовка "Каталог марок: Марка › Название" - берем только название
                var titleSelectors = ['h1', '.item_title', '.stamp_title', '[itemprop="name"]', '.item_name', 'h1.item_title', '.page_title', 'h1.page_title'];
                for (var i = 0; i < titleSelectors.length; i++) {
                    var titleEl = document.querySelector(titleSelectors[i]);
                    if (titleEl && titleEl.textContent.trim()) {
                        var titleText = titleEl.textContent.trim();
                        // Ищем паттерн "Каталог марок: Марка › Название" или "Марка › Название" - берем только название после "›"
                        // Используем символ › (U+203A) или обычный >
                        var nameMatch = titleText.match(/(?:Каталог марок\s*:\s*)?(?:Марка|Stamp)\s*[›>]\s*(.+)/i);
                        if (nameMatch && nameMatch[1]) {
                            var name = nameMatch[1].trim();
                            // Убираем любые символы < в начале
                            name = name.replace(/^<+\s*/, '');
                            result.name = name;
                        } else {
                            // Альтернативный вариант: ищем после последнего "›" или ">"
                            var lastArrow = Math.max(titleText.lastIndexOf('›'), titleText.lastIndexOf('>'));
                            if (lastArrow !== -1) {
                                var name = titleText.substring(lastArrow + 1).trim();
                                // Убираем любые символы < в начале
                                name = name.replace(/^<+\s*/, '');
                                result.name = name;
                            } else {
                                // Если нет "›" или ">", пробуем найти название после "Марка"
                                var stampMatch = titleText.match(/(?:Марка|Stamp)\s+(.+)/i);
                                if (stampMatch && stampMatch[1]) {
                                    var name = stampMatch[1].trim();
                                    name = name.replace(/^<+\s*/, '');
                                    result.name = name;
                                } else {
                                    var name = titleText;
                                    name = name.replace(/^<+\s*/, '');
                                    result.name = name;
                                }
                            }
                        }
                        break;
                    }
                }
                
                // Извлечение изображения марки - ищем единственное изображение на странице
                // Сначала ищем изображения, связанные с маркой
                var imgSelectors = [
                    'img[src*="/stamps/stamp/"]',
                    'img[src*="stamp"]',
                    '.stamp_image img',
                    '.item_image img',
                    '#item_image img',
                    '.item_main_image img',
                    '.large_image img',
                    'img.item_image',
                    'img[alt*="stamp"]',
                    'img[alt*="марка"]'
                ];
                
                var foundImage = false;
                for (var i = 0; i < imgSelectors.length; i++) {
                    var imgEl = document.querySelector(imgSelectors[i]);
                    if (imgEl) {
                        var imgUrl = imgEl.src || imgEl.getAttribute('data-src') || imgEl.getAttribute('data-lazy-src') || imgEl.getAttribute('data-original') || '';
                        // Пропускаем маленькие иконки и логотипы
                        if (imgUrl && 
                            imgUrl.indexOf('icon') === -1 && 
                            imgUrl.indexOf('logo') === -1 &&
                            imgUrl.indexOf('avatar') === -1) {
                            if (imgUrl.startsWith('//')) {
                                imgUrl = 'https:' + imgUrl;
                            } else if (!imgUrl.startsWith('http')) {
                                imgUrl = 'https://colnect.com' + (imgUrl.startsWith('/') ? imgUrl : '/' + imgUrl);
                            }
                            result.imageUrl = imgUrl;
                            foundImage = true;
                            break;
                        }
                    }
                }
                
                // Если не нашли специфичное изображение, ищем любое изображение на странице (кроме иконок)
                if (!foundImage) {
                    var allImages = document.querySelectorAll('img');
                    var bestImage = null;
                    var bestSize = 0;
                    
                    for (var i = 0; i < allImages.length; i++) {
                        var imgEl = allImages[i];
                        var imgUrl = imgEl.src || imgEl.getAttribute('data-src') || imgEl.getAttribute('data-lazy-src') || '';
                        
                        // Пропускаем маленькие изображения, иконки, логотипы
                        if (imgUrl && 
                            imgUrl.indexOf('icon') === -1 && 
                            imgUrl.indexOf('logo') === -1 &&
                            imgUrl.indexOf('avatar') === -1 &&
                            imgUrl.indexOf('flag') === -1) {
                            
                            // Проверяем размер изображения
                            var width = imgEl.width || imgEl.naturalWidth || 0;
                            var height = imgEl.height || imgEl.naturalHeight || 0;
                            var size = width * height;
                            
                            // Берем самое большое изображение (предположительно изображение марки)
                            if (size > bestSize && width > 50 && height > 50) {
                                if (imgUrl.startsWith('//')) {
                                    imgUrl = 'https:' + imgUrl;
                                } else if (!imgUrl.startsWith('http')) {
                                    imgUrl = 'https://colnect.com' + (imgUrl.startsWith('/') ? imgUrl : '/' + imgUrl);
                                }
                                bestImage = imgUrl;
                                bestSize = size;
                            }
                        }
                    }
                    
                    if (bestImage) {
                        result.imageUrl = bestImage;
                    }
                }
                
                // Универсальное извлечение информации из таблиц, списков и блоков деталей
                var detailEntries = [];
                
                // Табличные строки
                var tableRows = document.querySelectorAll('table tr');
                tableRows.forEach(function(row) {
                    var cells = row.querySelectorAll('td, th');
                    if (cells.length >= 2) {
                        detailEntries.push({
                            labelEl: cells[0],
                            valueEl: cells[1],
                            contextEl: row
                        });
                    }
                });
                
                // Элементы definition list (dt/dd)
                var definitionPairs = document.querySelectorAll('dl dt');
                definitionPairs.forEach(function(dtEl) {
                    var ddEl = dtEl.nextElementSibling;
                    if (ddEl && (ddEl.tagName === 'DD' || ddEl.tagName === 'DIV' || ddEl.tagName === 'SPAN')) {
                        detailEntries.push({
                            labelEl: dtEl,
                            valueEl: ddEl,
                            contextEl: dtEl.parentElement
                        });
                    }
                });
                
                // Общие блоки с классами, используемыми на Colnect
                var fieldContainers = document.querySelectorAll('.item_details .field, .item_details .item_detail, .item_info_data .row');
                fieldContainers.forEach(function(container) {
                    var labelCandidate = container.querySelector('.field_label, .field-title, .field_label_inner, .label, .title, dt');
                    var valueCandidate = container.querySelector('.field_content, .field-value, .value, dd, .content');
                    if (labelCandidate && valueCandidate) {
                        detailEntries.push({
                            labelEl: labelCandidate,
                            valueEl: valueCandidate,
                            contextEl: container
                        });
                    }
                });
                
                console.log('Total entries found: ' + detailEntries.length);
                
                var processLabelValue = function(labelEl, valueEl, contextEl) {
                    if (!labelEl || !valueEl) {
                        return;
                    }
                    var rawLabelText = (labelEl.textContent || '').trim();
                    var labelText = rawLabelText.replace(/[:：]+$/, '').trim().toLowerCase();
                    
                    if (!labelText) {
                        return;
                    }
                    
                    if (labelText.length > 0 && labelText.length < 80) {
                        console.log('Found label: "' + labelText + '"');
                    }
                    
                    var valueText = (valueEl.textContent || '').trim();
                    var contextText = contextEl ? (contextEl.textContent || '').trim() : (labelText + ' ' + valueText);
                    var contextLower = contextText.toLowerCase();
                    
                    // 1. Страна
                    if ((labelText.indexOf('страна') !== -1 || labelText.indexOf('country') !== -1) && !result.country) {
                        if (contextLower.indexOf('недавно измен') !== -1 ||
                            contextLower.indexOf('recently change') !== -1 ||
                            contextLower.indexOf('недавно добав') !== -1 ||
                            contextLower.indexOf('recently add') !== -1) {
                            console.log('Skipping country due to recently changed row: ' + contextText);
                        } else if (valueText && valueText.length > 0) {
                            result.country = valueText;
                            console.log('Set country to: ' + result.country);
                        }
                    }
                    
                    // 3. Описание
                    if ((labelText === 'темы' || labelText === 'themes' || labelText === 'theme') && !result.description) {
                        var descriptionText = '';
                        var descriptionLinks = valueEl.querySelectorAll('a');
                        if (descriptionLinks.length > 0) {
                            var linkTexts = [];
                            descriptionLinks.forEach(function(link) {
                                var linkText = link.textContent.trim();
                                if (linkText) {
                                    linkTexts.push(linkText);
                                }
                            });
                            if (linkTexts.length > 0) {
                                descriptionText = linkTexts.join(' | ');
                            }
                        }
                        if (!descriptionText) {
                            descriptionText = valueText;
                        }
                        if (descriptionText && descriptionText.length > 0) {
                            result.description = descriptionText;
                        }
                    }
                    
                    // 4. Год
                    if ((labelText.indexOf('дата выпуска') !== -1 || labelText.indexOf('release date') !== -1) && !result.year) {
                        var yearMatch = valueText.match(/\b(17|18|19|20)\d{2}\b/);
                        if (yearMatch) {
                            result.year = yearMatch[0];
                        }
                    }
                    
                    // 5. Номинал
                    if ((labelText.indexOf('номинальная стоимость') !== -1 || labelText.indexOf('face value') !== -1) && !result.denomination) {
                        var denominationValue = valueText;
                        if (!denominationValue || denominationValue.length === 0) {
                            var denomLinks = valueEl.querySelectorAll('a');
                            if (denomLinks.length > 0) {
                                var denomTexts = [];
                                denomLinks.forEach(function(link) {
                                    var linkText = link.textContent.trim();
                                    if (linkText) {
                                        denomTexts.push(linkText);
                                    }
                                });
                                if (denomTexts.length > 0) {
                                    denominationValue = denomTexts.join(' ');
                                }
                            }
                        }
                        if (denominationValue && denominationValue.length > 0) {
                            result.denomination = denominationValue;
                        }
                    }
                    
                    // 6. Цена
                    if ((labelText.indexOf('купить сейчас') !== -1 || labelText.indexOf('buy now') !== -1) && !result.price) {
                        var priceText = valueText;
                        var priceMatch = priceText.match(/(?:from|от)\s+([A-Z]{2,3}\$|[€$₽£¥])?\s*([\d]+[,\.\d]*)\s*([€$₽£¥]|[A-Z]{2,3})?/i);
                        if (!priceMatch) {
                            priceMatch = priceText.match(/([\d]+[,\.\d]*)\s*([€$₽£¥]|USD|EUR|RUB)/i);
                        }
                        if (priceMatch) {
                            var currencyToken = '';
                            if (priceMatch[1]) {
                                currencyToken = priceMatch[1];
                            } else if (priceMatch[3]) {
                                currencyToken = priceMatch[3];
                            }
                            var priceValue = priceMatch[2] ? priceMatch[2].replace(',', '.').trim() : '';
                            if (priceValue) {
                                result.price = priceValue;
                                if (!result.priceCurrency) {
                                    var currencyGuess = '€';
                                    var tokenUpper = currencyToken ? currencyToken.toUpperCase() : '';
                                    if (tokenUpper.indexOf('US') !== -1 || tokenUpper.indexOf('$') !== -1) {
                                        currencyGuess = '$';
                                    } else if (tokenUpper.indexOf('€') !== -1 || tokenUpper.indexOf('EUR') !== -1) {
                                        currencyGuess = '€';
                                    } else if (tokenUpper.indexOf('₽') !== -1 || tokenUpper.indexOf('RUB') !== -1) {
                                        currencyGuess = '₽';
                                    }
                                    result.priceCurrency = currencyGuess;
                                }
                            }
                        }
                    }
                };
                
                detailEntries.forEach(function(entry) {
                    processLabelValue(entry.labelEl, entry.valueEl, entry.contextEl);
                });
                
                // 6. Извлечение цены - ищем "Купить сейчас", после слова "from" и знак валюты
                // Ищем элемент, содержащий текст "Купить сейчас"
                var buyNowText = '';
                var allTextElements = document.querySelectorAll('*');
                for (var i = 0; i < allTextElements.length; i++) {
                    var el = allTextElements[i];
                    var text = el.textContent || '';
                    if (text.indexOf('Купить сейчас') !== -1 || text.indexOf('Buy now') !== -1) {
                        buyNowText = text;
                        break;
                    }
                }
                
                // Если не нашли через "Купить сейчас", ищем через "sale offers"
                if (!buyNowText) {
                    for (var i = 0; i < allTextElements.length; i++) {
                        var el = allTextElements[i];
                        var text = el.textContent || '';
                        if (text.indexOf('sale offers') !== -1 && text.indexOf('from') !== -1) {
                            buyNowText = text;
                            break;
                        }
                    }
                }
                
                if (buyNowText) {
                    // Ищем паттерн "from US$ 0,06" или "from € 0,06"
                    var fromMatch = buyNowText.match(/(?:from|от)\s+([A-Z]{2,3}\s*)?\$?\s*([\d,]+\.?\d{0,2})/i);
                    if (fromMatch && fromMatch[2]) {
                        var priceValue = fromMatch[2].replace(',', '.');
                        var currencySymbol = fromMatch[1] ? fromMatch[1].trim() : '';
                        // Определяем валюту
                        var currency = '€';
                        if (currencySymbol.indexOf('US') !== -1 || buyNowText.indexOf('US$') !== -1) {
                            currency = '$';
                        } else if (currencySymbol.indexOf('EUR') !== -1 || buyNowText.indexOf('€') !== -1) {
                            currency = '€';
                        } else if (currencySymbol.indexOf('RUB') !== -1 || buyNowText.indexOf('₽') !== -1) {
                            currency = '₽';
                        }
                        result.price = priceValue;
                        result.priceCurrency = currency;
                    }
                }
                
                
                if (!result.year) {
                    // Ищем год в тексте страницы
                    var bodyText = document.body.textContent || '';
                    var yearMatches = bodyText.match(/\b(19|20)\d{2}\b/g);
                    if (yearMatches && yearMatches.length > 0) {
                        // Берем первый разумный год (не текущий и не слишком старый)
                        for (var i = 0; i < yearMatches.length; i++) {
                            var y = parseInt(yearMatches[i]);
                            if (y >= 1800 && y <= 2100) {
                                result.year = yearMatches[i];
                                break;
                            }
                        }
                    }
                }
                
            } catch(e) {
                console.error('Parse detail error: ' + e.message);
            }
            
            // Отладочный вывод результата
            console.log('Final parsing result - country: ' + result.country + ', name: ' + result.name);
            
            // Используем JSON.stringify с правильной обработкой Unicode
            return JSON.stringify(result, null, 0);
        })();
    """.trimIndent()
    
    // Функция для проверки входа
    fun performLoginCheck(view: WebView?, currentUrl: String?) {
        if (currentUrl?.contains("colnect.com") == true && !currentUrl.contains("/stamps/stamp/") && !isLoggedIn && checkLoginAttempts < 5) {
            view?.evaluateJavascript(checkLoginJavaScript) { result ->
                try {
                    val isLoggedInResult = result.removeSurrounding("\"")
                        .replace("\\\"", "\"")
                        .replace("\\n", "")
                        .replace("\\\\", "")
                    val loggedIn = isLoggedInResult == "true"
                    
                    if (loggedIn) {
                        isLoggedIn = true
                        if (!hasLoggedInBefore) {
                            hasLoggedInBefore = true
                            loginPreferences.edit().putBoolean("colnect_logged_in", true).apply()
                        }
                        if (showLoginScreen) {
                            showLoginScreen = false
                        }
                    } else {
                        // Увеличиваем счетчик попыток
                        checkLoginAttempts++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // Функция для обработки парсинга детальной страницы марки
    val handleStampDetailParsing = { view: WebView?, url: String? ->
        if (url?.contains("/stamps/stamp/") == true) {
            view?.evaluateJavascript(parseStampDetailJavaScript) { result ->
                try {
                    // Обрабатываем JSON строку с правильной кодировкой UTF-8
                    var jsonString = result.removeSurrounding("\"")
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t")
                        .replace("\\\\", "\\")
                    // Исправляем Unicode escape последовательности
                    jsonString = jsonString.replace("\\u", "\\u")
                    
                    if (jsonString.isNotBlank() && jsonString != "null" && jsonString.startsWith("{")) {
                        val obj = org.json.JSONObject(jsonString)
                        // Получаем текущий URL страницы марки
                        val stampPageUrl = url ?: ""
                        // Определяем валюту для цены (только если цена есть и не пустая)
                        val priceValue = obj.optString("price", "").trim()
                        val priceCurrency = if (priceValue.isNotBlank() && priceValue != "." && priceValue != "0" && priceValue.toDoubleOrNull() != null) {
                            obj.optString("priceCurrency", "").let { currency ->
                                when {
                                    currency.contains("€", ignoreCase = true) || currency.isEmpty() -> "EUR"
                                    currency.contains("$", ignoreCase = true) || currency.contains("US", ignoreCase = true) -> "USD"
                                    currency.contains("₽", ignoreCase = true) -> "RUB"
                                    else -> "EUR" // По умолчанию евро
                                }
                            }
                        } else {
                            ""
                        }
                        
                        // Очищаем цену от лишних символов
                        val cleanPrice = if (priceValue.isNotBlank() && priceValue != "." && priceValue != "0" && priceValue.toDoubleOrNull() != null) {
                            priceValue
                        } else {
                            ""
                        }
                        
                        // Извлекаем данные с правильной обработкой UTF-8
                        val stampData = mapOf(
                            "name" to (obj.optString("name", "") ?: "").trim(), // 2 - Название из заголовка
                            "country" to (obj.optString("country", "") ?: "").trim(), // 1 - Страна
                            "year" to (obj.optString("year", "") ?: "").trim(), // 4 - Год из "Дата выпуска"
                            "denomination" to (obj.optString("denomination", "") ?: "").trim(), // 5 - Номинал из "Номинальная стоимость"
                            "description" to (obj.optString("description", "") ?: "").trim(), // 3 - Описание из "Темы"
                            "price" to cleanPrice, // 6 - Цена из "Купить сейчас" (пусто, если нет)
                            "currency" to priceCurrency, // Валюта для цены (пусто, если нет цены)
                            "imageUrl" to (obj.optString("imageUrl", "") ?: "").trim(),
                            "priceUrl" to stampPageUrl, // Ссылка на страницу марки
                            "detailUrl" to stampPageUrl
                        )
                        
                        // Отладочный вывод для проверки данных
                        android.util.Log.d("StampParsing", "Country: ${stampData["country"]}")
                        android.util.Log.d("StampParsing", "Name: ${stampData["name"]}")
                        // Возвращаем данные для редактирования и закрываем WebView
                        onStampSelected(stampData)
                        // Закрываем WebView после получения данных
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(300)
                            onBack()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // Экран входа в Colnect (показывается только при первом открытии)
    if (showLoginScreen) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Вход в Colnect") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Вход в Colnect",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Для работы с каталогом марок Colnect необходимо войти в свой аккаунт. Сайт не работает нормально без входа.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Если у вас нет аккаунта, пожалуйста, зарегистрируйтесь на сайте Colnect.com",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Button(
                    onClick = {
                        showLoginScreen = false
                        // Начинаем проверку входа после загрузки страницы
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                ) {
                    Text("Перейти на сайт Colnect")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = {
                        android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://colnect.com/ru/register")
                            context.startActivity(this)
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Зарегистрироваться на Colnect.com")
                }
            }
        }
        return
    }
    
    // WebView с сайтом Colnect
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Каталог марок Colnect") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var webViewRef by remember { mutableStateOf<WebView?>(null) }
            var webViewInitialized by remember { mutableStateOf(false) }
            
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        webViewInitialized = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                
                                // Если это детальная страница марки, парсим её и закрываем WebView
                                if (url?.contains("/stamps/stamp/") == true) {
                                    view?.postDelayed({
                                        handleStampDetailParsing(view, url)
                                    }, 1500)
                                } else if (url?.contains("colnect.com") == true) {
                                    // Проверяем вход после загрузки страницы (только если не детальная страница)
                                    view?.postDelayed({
                                        performLoginCheck(view, url)
                                    }, 1000)
                                }
                            }
                            
                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                // Разрешаем все переходы внутри Colnect
                                val requestUrl = request?.url?.toString()
                                if (requestUrl?.contains("/stamps/stamp/") == true) {
                                    currentStampDetailUrl = requestUrl
                                    isLoading = true
                                }
                                return false
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.userAgentString = settings.userAgentString + " Mobile"
                        settings.defaultTextEncodingName = "UTF-8"
                        
                        // Загружаем начальную страницу или детальную страницу марки
                        when {
                            currentStampDetailUrl != null -> {
                                loadUrl(currentStampDetailUrl ?: url)
                            }
                            else -> {
                                loadUrl(url)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            
            // Отслеживаем изменения currentStampDetailUrl для загрузки детальной страницы
            LaunchedEffect(currentStampDetailUrl, webViewInitialized) {
                if (currentStampDetailUrl != null && webViewRef != null && webViewInitialized) {
                    kotlinx.coroutines.delay(100)
                    webViewRef?.loadUrl(currentStampDetailUrl ?: "")
                    isLoading = true
                }
            }
            
            // Периодическая проверка входа (если еще не вошли)
            LaunchedEffect(checkLoginAttempts, isLoggedIn, webViewRef) {
                if (!isLoggedIn && checkLoginAttempts < 5 && webViewRef != null && !showLoginScreen) {
                    kotlinx.coroutines.delay(2000)
                    val currentUrl = webViewRef?.url
                    if (currentUrl != null && currentUrl.contains("colnect.com") && !currentUrl.contains("/stamps/stamp/")) {
                        performLoginCheck(webViewRef, currentUrl)
                    }
                }
            }
            
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StampSearchResultItem(
    stamp: StampSearchResult,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Изображение марки
            if (stamp.imageUrl != null && stamp.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = stamp.imageUrl,
                        error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                    ),
                    contentDescription = "Марка",
                    modifier = Modifier
                        .size(80.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (stamp.country.isNotBlank()) stamp.country else "Неизвестная страна",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (stamp.year.isNotBlank()) {
                    Text(
                        text = "Год: ${stamp.year}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (stamp.denomination != null && stamp.denomination.isNotBlank()) {
                    Text(
                        text = "Номинал: ${stamp.denomination}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (stamp.description != null && stamp.description.isNotBlank()) {
                    Text(
                        text = stamp.description,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                if (stamp.price != null && stamp.price.isNotBlank()) {
                    Text(
                        text = "Цена: ${stamp.price}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

