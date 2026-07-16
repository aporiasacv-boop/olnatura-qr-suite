import { useCallback, useEffect, useId, useRef, useState } from "react";
import { Input, makeStyles, shorthands, Text } from "@fluentui/react-components";
import { api } from "../../api/client";
import { brand } from "../../styles/brand";

export type LoteSuggestion = {
  lote: string;
  codigo: string;
  nombre: string;
  status: string | null;
};

type Props = {
  value: string;
  onChange: (value: string) => void;
  onSelect?: (item: LoteSuggestion) => void;
  placeholder?: string;
  disabled?: boolean;
  readOnly?: boolean;
  id?: string;
  name?: string;
  appearance?: "outline" | "underline" | "filled-darker" | "filled-lighter";
  size?: "small" | "medium" | "large";
  className?: string;
  style?: React.CSSProperties;
  onKeyDown?: (e: React.KeyboardEvent<HTMLInputElement>) => void;
  /** Debounce en ms (default 250). */
  debounceMs?: number;
};

const useStyles = makeStyles({
  wrap: {
    position: "relative",
    width: "100%",
    minWidth: 0,
  },
  list: {
    position: "absolute",
    zIndex: 40,
    left: 0,
    right: 0,
    top: "calc(100% + 4px)",
    maxHeight: "280px",
    overflowY: "auto",
    backgroundColor: brand.surfaceSolid,
    ...shorthands.border("1px", "solid", brand.border),
    borderRadius: "10px",
    boxShadow: "0 8px 24px rgba(74, 92, 40, 0.12)",
    ...shorthands.padding("4px"),
  },
  item: {
    display: "grid",
    gap: "2px",
    ...shorthands.padding("10px", "12px"),
    borderRadius: "8px",
    cursor: "pointer",
    transition: "background-color 0.15s ease",
  },
  itemActive: {
    backgroundColor: brand.primarySoft,
  },
  itemHover: {
    ":hover": {
      backgroundColor: "rgba(239, 241, 161, 0.55)",
    },
  },
  lote: {
    fontWeight: 700,
    fontSize: "13px",
    color: brand.text,
  },
  meta: {
    fontSize: "12px",
    color: brand.muted,
    lineHeight: 1.35,
  },
  status: {
    fontSize: "11px",
    fontWeight: 600,
    color: brand.text2,
  },
  empty: {
    ...shorthands.padding("10px", "12px"),
    fontSize: "12px",
    color: brand.muted,
  },
});

function clsx(...parts: Array<string | false | null | undefined>) {
  return parts.filter(Boolean).join(" ");
}

/**
 * Autocompletado de lotes desde PostgreSQL (sin Dynamics).
 */
export default function LoteAutocomplete({
  value,
  onChange,
  onSelect,
  placeholder = "Buscar lote…",
  disabled,
  readOnly,
  id,
  name,
  appearance = "outline",
  size = "large",
  className,
  style,
  onKeyDown,
  debounceMs = 250,
}: Props) {
  const s = useStyles();
  const listId = useId();
  const wrapRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<LoteSuggestion[]>([]);
  const [activeIdx, setActiveIdx] = useState(0);
  const [loading, setLoading] = useState(false);
  const skipFetchRef = useRef(false);
  const autoSelectedRef = useRef<string | null>(null);

  const pick = useCallback(
    (item: LoteSuggestion) => {
      skipFetchRef.current = true;
      autoSelectedRef.current = item.lote;
      onChange(item.lote);
      onSelect?.(item);
      setOpen(false);
      setItems([]);
      setActiveIdx(0);
    },
    [onChange, onSelect]
  );

  useEffect(() => {
    if (readOnly || disabled) {
      setOpen(false);
      setItems([]);
      return;
    }
    if (skipFetchRef.current) {
      skipFetchRef.current = false;
      return;
    }

    const q = value.trim();
    if (q.length < 1) {
      setItems([]);
      setOpen(false);
      setLoading(false);
      return;
    }

    let cancelled = false;
    const t = window.setTimeout(() => {
      setLoading(true);
      api<LoteSuggestion[]>(`/labels/suggest?q=${encodeURIComponent(q)}`, { toast: false })
        .then((rows) => {
          if (cancelled) return;
          const list = Array.isArray(rows) ? rows : [];
          setItems(list);
          setActiveIdx(0);

          const exact =
            list.length === 1 &&
            list[0].lote.trim().toLowerCase() === q.toLowerCase();
          if (exact && autoSelectedRef.current !== list[0].lote) {
            pick(list[0]);
            return;
          }
          setOpen(list.length > 0);
        })
        .catch(() => {
          if (cancelled) return;
          setItems([]);
          setOpen(false);
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    }, debounceMs);

    return () => {
      cancelled = true;
      window.clearTimeout(t);
    };
  }, [value, debounceMs, readOnly, disabled, pick]);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (!wrapRef.current?.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (open && items.length > 0) {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveIdx((i) => (i + 1) % items.length);
        return;
      }
      if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveIdx((i) => (i - 1 + items.length) % items.length);
        return;
      }
      if (e.key === "Enter" && items[activeIdx]) {
        e.preventDefault();
        pick(items[activeIdx]);
        return;
      }
      if (e.key === "Escape") {
        e.preventDefault();
        setOpen(false);
        return;
      }
    }
    onKeyDown?.(e);
  };

  return (
    <div className={s.wrap} ref={wrapRef} style={style}>
      <Input
        id={id}
        name={name}
        appearance={appearance}
        size={size}
        className={className}
        value={value}
        disabled={disabled}
        readOnly={readOnly}
        placeholder={placeholder}
        role="combobox"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        aria-autocomplete="list"
        onChange={(_, d) => {
          autoSelectedRef.current = null;
          onChange(d.value);
          if (!readOnly && !disabled) setOpen(true);
        }}
        onFocus={() => {
          if (!readOnly && !disabled && items.length > 0) setOpen(true);
        }}
        onKeyDown={handleKeyDown}
      />
      {open && !readOnly && !disabled ? (
        <div id={listId} className={s.list} role="listbox">
          {loading && items.length === 0 ? (
            <div className={s.empty}>Buscando…</div>
          ) : items.length === 0 ? (
            <div className={s.empty}>Sin coincidencias en base de datos</div>
          ) : (
            items.map((item, idx) => (
              <div
                key={`${item.lote}-${idx}`}
                role="option"
                aria-selected={idx === activeIdx}
                className={clsx(s.item, s.itemHover, idx === activeIdx && s.itemActive)}
                onMouseEnter={() => setActiveIdx(idx)}
                onMouseDown={(e) => {
                  e.preventDefault();
                  pick(item);
                }}
              >
                <Text className={s.lote}>{item.lote}</Text>
                <Text className={s.meta}>
                  {item.codigo || "—"} · {item.nombre || "—"}
                </Text>
                {item.status ? <Text className={s.status}>{item.status}</Text> : null}
              </div>
            ))
          )}
        </div>
      ) : null}
    </div>
  );
}
