import { useState } from 'react';
import { validatePin } from '../api/client';

interface ConsentScreenProps {
  onComplete: (pin: string, consented: boolean) => void;
}

export default function ConsentScreen({ onComplete }: ConsentScreenProps) {
  const [pin, setPin]         = useState('');
  const [error, setError]     = useState('');
  const [loading, setLoading] = useState(false);

  const handlePinSubmit = async () => {
    if (pin.length !== 4) return;
    setLoading(true);
    setError('');
    const valid = await validatePin(pin);
    if (valid) {
      onComplete(pin, true);
    } else {
      setError('Invalid pin number.');
    }
    setLoading(false);
  };

  return (
    <div style={{
      width: '100%', height: '100%',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: '#0ca6e9',
    }}>
      <div style={{
        background: '#fff', borderRadius: 12,
        borderTop:    '1.5px solid #1E293B',
        borderLeft:   '1.5px solid #1E293B',
        borderRight:  '4px solid #1E293B',
        borderBottom: '4px solid #1E293B',
        boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
        padding: '40px 48px', width: 360, textAlign: 'center',
      }}>
        <div style={{
          display: 'inline-block',
          fontFamily: 'Helvetica, Arial, sans-serif',
          fontWeight: 800,
          fontSize: 30,
          color: '#1E293B',
          background: '#d9f9ff',
          padding: '10px 14px',
          borderRadius: 8,
          borderTop:    '1.5px solid #1E293B',
          borderLeft:   '1.5px solid #1E293B',
          borderRight:  '4px solid #1E293B',
          borderBottom: '4px solid #1E293B',
        }}>
          Scaffold
        </div>
        <div style={{ fontSize: 14, color: '#64748B', marginBottom: 20, marginTop: 20 }}>
          Enter your assigned 4-digit pin to continue.
        </div>
        <input
          type="text"
          inputMode="numeric"
          maxLength={4}
          value={pin}
          onChange={e => setPin(e.target.value.replace(/\D/g, ''))}
          onKeyDown={e => e.key === 'Enter' && handlePinSubmit()}
          placeholder="0000"
          style={{
            width: '100%', padding: '10px 14px', fontSize: 24,
            textAlign: 'center', letterSpacing: '0.3em',
            border: '1px solid #CBD5E1', borderRadius: 8,
            outline: 'none', marginBottom: 12,
            boxSizing: 'border-box',
          }}
        />
        {error && (
          <div style={{ color: '#DC2626', fontSize: 13, marginBottom: 12 }}>{error}</div>
        )}
        <button
          onClick={handlePinSubmit}
          disabled={pin.length !== 4 || loading}
          style={{
            width: '100%', padding: '10px 0', fontSize: 14, fontWeight: 600,
            background: pin.length === 4 ? '#1E293B' : '#CBD5E1',
            color: '#fff', border: 'none', borderRadius: 8,
            cursor: pin.length === 4 ? 'pointer' : 'default',
          }}
        >
          {loading ? 'Checking…' : 'Continue'}
        </button>
      </div>
    </div>
  );
}