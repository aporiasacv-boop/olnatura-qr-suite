import { Button, Tooltip, makeStyles, shorthands } from "@fluentui/react-components";
import { brand } from "../../styles/brand";

const useStyles = makeStyles({
  fieldBox: {
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("10px"),
    ...shorthands.padding("10px"),
  },
  fieldLabel: { color: brand.muted, fontSize: "12px" },
  fieldValue: { marginTop: "4px", fontWeight: 600, wordBreak: "break-word" },
  plainBlock: { display: "grid", gap: "2px" },
  plainLabel: { fontSize: "11px", color: brand.muted, fontWeight: 600 },
  plainValue: {
    fontSize: "13px",
    color: brand.text,
    fontWeight: 600,
    wordBreak: "break-word",
  },
});

export function PlainField({ label, value }: { label: string; value: string }) {
  const s = useStyles();
  return (
    <div className={s.plainBlock}>
      <div className={s.plainLabel}>{label}</div>
      <div className={s.plainValue}>{value}</div>
    </div>
  );
}

export function CopyField({
  label,
  value,
  onCopy,
  boxed = true,
}: {
  label: string;
  value: string;
  onCopy: (label: string, value: string) => void;
  boxed?: boolean;
}) {
  const s = useStyles();
  return (
    <div className={boxed ? s.fieldBox : s.plainBlock}>
      <div className={boxed ? s.fieldLabel : s.plainLabel}>{label}</div>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 8,
          marginTop: boxed ? 4 : 0,
        }}
      >
        <div
          className={boxed ? s.fieldValue : s.plainValue}
          style={{ marginTop: 0, flex: 1 }}
        >
          {value}
        </div>
        {value && value !== "—" ? (
          <Tooltip content="Copiar" relationship="label">
            <Button size="small" appearance="subtle" onClick={() => onCopy(label, value)}>
              Copiar
            </Button>
          </Tooltip>
        ) : null}
      </div>
    </div>
  );
}
