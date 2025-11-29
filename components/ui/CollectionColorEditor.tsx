'use client';

import React, { useEffect, useState } from 'react';

export type CollectionColorEditorProps = {
  initialColor?: string;
  onSave: (color: string) => Promise<void> | void;
  disabled?: boolean;
  className?: string;
};

export const CollectionColorEditor: React.FC<CollectionColorEditorProps> = ({
  initialColor = '#888888',
  onSave,
  disabled = false,
  className,
}) => {
  const [color, setColor] = useState<string>(initialColor);
  const [saving, setSaving] = useState<boolean>(false);

  useEffect(() => {
    setColor(initialColor);
  }, [initialColor]);

  const handleSave = async (): Promise<void> => {
    if (saving || disabled) return;
    setSaving(true);
    try {
      await onSave(color);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className={className}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <input
          type="color"
          value={color}
          onChange={(e) => setColor(e.target.value)}
          aria-label="Collection color"
          disabled={disabled || saving}
        />
        <div
          aria-hidden
          style={{
            width: 24,
            height: 24,
            borderRadius: '50%',
            backgroundColor: color,
            border: '1px solid #ccc',
          }}
        />
        <button
          type="button"
          onClick={() => void handleSave()}
          disabled={disabled || saving}
          style={{
            padding: '6px 10px',
            borderRadius: 6,
            border: '1px solid #ccc',
            background: '#f9f9f9',
            cursor: disabled || saving ? 'not-allowed' : 'pointer',
          }}
        >
          {saving ? 'Saving...' : 'Save'}
        </button>
      </div>
    </div>
  );
};

export default CollectionColorEditor;
