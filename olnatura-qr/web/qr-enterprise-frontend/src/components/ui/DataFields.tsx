import { Button, Tooltip, makeStyles, shorthands } from "@fluentui/react-components";
import { brand } from "../../styles/brand";

const useStyles = makeStyles({
  fieldBox: {
    backgroundColor: "#FFFFFF",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("10px"),
    ...shorthands.padding("8px", "10px"),
    display: "grid",
    gap: "2px",
  },
  fieldLabel: {
    color: brand.muted,
    fontSize: "11px",
    fontWeight: 600,
  },
  fieldValue: {
    fontSize: "13px",
    lineHeight: "1.3",
    color: brand.text,
    fontWeight: 600,
    wordBreak: "break-word",
  },
  plainBlock: {
    display: "grid",
    gap: "3px",
  },
  plainLabel: {
    fontSize: "12px",
    color: "#5A6570",
    fontWeight: 500,
    lineHeight: "1.3",
  },
  plainValue: {
    fontSize: "15px",
    color: "#1A2330",
    fontWeight: 700,
    lineHeight: "1.35",
    wordBreak: "break-word",
  },
});

export function PlainField({
  label,
  value,
  boxed = true,
}: {
  label: string;
  value: string;
  boxed?: boolean;
}) {
  const s = useStyles();
  return (
    <div className={boxed ? s.fieldBox : s.plainBlock}>
      <div className={boxed ? s.fieldLabel : s.plainLabel}>{label}</div>
      <div className={boxed ? s.fieldValue : s.plainValue}>{value}</div>
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
          gap: 6,
        }}
      >
        <div className={boxed ? s.fieldValue : s.plainValue} style={{ flex: 1 }}>
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
