import { useCallback, useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
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
  /** Estilos del contenedor (flex, width). No uses border aquí: crea doble caja. */
  style?: React.CSSProperties;
  /** Estilos del Input (borde de atención, fondo Dynamics, etc.). */
  inputStyle?: React.CSSProperties;
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
    position: "fixed",
    zIndex: 10000,
    maxHeight: "280px",
    overflowY: "auto",
    backgroundColor: brand.surfaceSolid,
    ...shorthands.border("1px", "solid", brand.border),
    borderRadius: "10px",
    boxShadow: "0 8px 24px rgba(74, 92, 40, 0.16)",
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
      backgroundColor: "rgba(226, 230, 168, 0.65)",
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
 * Cierra el desplegable al seleccionar, blur o Enter de formulario (sin fantasmas).
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
  inputStyle,
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
  const [listBox, setListBox] = useState<{ top: number; left: number; width: number } | null>(null);
  const skipFetchRef = useRef(false);
  /** Tras elegir/auto-seleccionar, no reabrir hasta que el usuario escriba. */
  const suppressOpenRef = useRef(false);
  const autoSelectedRef = useRef<string | null>(null);
  const blurTimerRef = useRef<number | null>(null);

  const closeList = useCallback(() => {
    setOpen(false);
    setItems([]);
    setActiveIdx(0);
    setListBox(null);
  }, []);

  const updateListPosition = useCallback(() => {
    const el = wrapRef.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    setListBox({
      top: r.bottom + 4,
      left: r.left,
      width: r.width,
    });
  }, []);

  const pick = useCallback(
    (item: LoteSuggestion) => {
      skipFetchRef.current = true;
      suppressOpenRef.current = true;
      autoSelectedRef.current = item.lote;
      onChange(item.lote);
      onSelect?.(item);
      closeList();
    },
    [onChange, onSelect, closeList]
  );

  useEffect(() => {
    if (readOnly || disabled) {
      closeList();
      return;
    }
    if (skipFetchRef.current) {
      skipFetchRef.current = false;
      return;
    }

    const q = value.trim();
    if (q.length < 1) {
      closeList();
      setLoading(false);
      return;
    }

    if (suppressOpenRef.current) {
      return;
    }

    let cancelled = false;
    const t = window.setTimeout(() => {
      setLoading(true);
      api<LoteSuggestion[]>(`/labels/suggest?q=${encodeURIComponent(q)}`, { toast: false })
        .then((rows) => {
          if (cancelled || suppressOpenRef.current) return;
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
          if (list.length > 0) {
            updateListPosition();
            setOpen(true);
          } else {
            setOpen(false);
            setListBox(null);
          }
        })
        .catch(() => {
          if (cancelled) return;
          closeList();
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    }, debounceMs);

    return () => {
      cancelled = true;
      window.clearTimeout(t);
    };
  }, [value, debounceMs, readOnly, disabled, pick, closeList, updateListPosition]);

  useEffect(() => {
    if (!open) return;
    updateListPosition();
    const onScroll = () => updateListPosition();
    window.addEventListener("scroll", onScroll, true);
    window.addEventListener("resize", onScroll);
    return () => {
      window.removeEventListener("scroll", onScroll, true);
      window.removeEventListener("resize", onScroll);
    };
  }, [open, updateListPosition]);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      const t = e.target as Node;
      if (wrapRef.current?.contains(t)) return;
      const portal = document.getElementById(listId);
      if (portal?.contains(t)) return;
      closeList();
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [closeList, listId]);

  useEffect(() => {
    return () => {
      if (blurTimerRef.current) window.clearTimeout(blurTimerRef.current);
    };
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
        closeList();
        return;
      }
    }
    // Enter del formulario: cerrar sugerencias para no dejar fantasma
    if (e.key === "Enter") {
      suppressOpenRef.current = true;
      closeList();
    }
    onKeyDown?.(e);
  };

  const list =
    open && !readOnly && !disabled && listBox
      ? createPortal(
          <div
            id={listId}
            className={s.list}
            role="listbox"
            style={{
              top: listBox.top,
              left: listBox.left,
              width: listBox.width,
            }}
          >
            {loading && items.length === 0 ? (
              <div className={s.empty}>Buscando…</div>
            ) : items.length === 0 ? (
              <div className={s.empty}>Sin coincidencias</div>
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
          </div>,
          document.body
        )
      : null;

  return (
    <div className={s.wrap} ref={wrapRef} style={style}>
      <Input
        id={id}
        name={name}
        appearance={appearance}
        size={size}
        className={className}
        style={inputStyle}
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
          suppressOpenRef.current = false;
          onChange(d.value);
        }}
        onFocus={() => {
          if (blurTimerRef.current) {
            window.clearTimeout(blurTimerRef.current);
            blurTimerRef.current = null;
          }
        }}
        onBlur={() => {
          blurTimerRef.current = window.setTimeout(() => {
            closeList();
          }, 120);
        }}
        onKeyDown={handleKeyDown}
      />
      {list}
    </div>
  );
}
