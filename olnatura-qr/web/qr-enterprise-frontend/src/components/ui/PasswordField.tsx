import { useState } from "react";
import { Button, Input, makeStyles } from "@fluentui/react-components";
import { EyeRegular, EyeOffRegular } from "@fluentui/react-icons";

const useStyles = makeStyles({
  input: {
    width: "100%",
    minWidth: 0,
  },
  toggle: {
    minWidth: "32px",
    height: "32px",
    padding: 0,
    cursor: "pointer",
    // Evita que el hover global (translateY) desplace el ojo fuera del input
    ":hover": {
      transform: "none",
      opacity: 1,
      filter: "none",
    },
    ":active": {
      transform: "none",
    },
  },
});

type PasswordFieldProps = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  id?: string;
  name?: string;
  className?: string;
  appearance?: "outline" | "underline" | "filled-darker" | "filled-lighter";
  size?: "small" | "medium" | "large";
  autoComplete?: string;
};

/** Campo de contraseña con icono ojo para mostrar/ocultar. */
export default function PasswordField({
  value,
  onChange,
  placeholder = "Contraseña",
  disabled,
  id,
  name,
  className,
  appearance = "outline",
  size = "large",
  autoComplete = "current-password",
}: PasswordFieldProps) {
  const s = useStyles();
  const [visible, setVisible] = useState(false);

  return (
    <Input
      id={id}
      name={name}
      appearance={appearance}
      size={size}
      className={className ? `${s.input} ${className}` : s.input}
      type={visible ? "text" : "password"}
      value={value}
      disabled={disabled}
      placeholder={placeholder}
      onChange={(_, d) => onChange(d.value)}
      autoComplete={autoComplete}
      contentAfter={
        <Button
          type="button"
          appearance="transparent"
          className={`${s.toggle} password-field-toggle`}
          aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
          title={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
          disabled={disabled}
          icon={visible ? <EyeOffRegular /> : <EyeRegular />}
          onClick={() => setVisible((v) => !v)}
        />
      }
    />
  );
}
