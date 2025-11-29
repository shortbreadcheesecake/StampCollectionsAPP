'use client';

import React from 'react';

export type FavoriteToggleProps = {
  isFavorite: boolean;
  onToggle: (next: boolean) => void;
  size?: number;
  className?: string;
  ariaLabel?: string;
};

export const FavoriteToggle: React.FC<FavoriteToggleProps> = ({
  isFavorite,
  onToggle,
  size = 20,
  className,
  ariaLabel,
}) => {
  return (
    <button
      type="button"
      aria-pressed={isFavorite}
      aria-label={ariaLabel ?? (isFavorite ? 'Remove from favorites' : 'Add to favorites')}
      onClick={() => onToggle(!isFavorite)}
      className={className}
      title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 0,
        background: 'transparent',
        border: 'none',
        cursor: 'pointer',
        color: isFavorite ? 'crimson' : 'currentColor',
      }}
    >
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill={isFavorite ? 'currentColor' : 'none'}
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="M20.84 4.61c-1.54-1.34-3.77-1.34-5.31 0L12 8.09 8.47 4.61c-1.54-1.34-3.77-1.34-5.31 0-1.84 1.6-1.84 4.2 0 5.8L12 21.35l8.84-10.94c1.84-1.6 1.84-4.2 0-5.8z" />
      </svg>
    </button>
  );
};

export default FavoriteToggle;
