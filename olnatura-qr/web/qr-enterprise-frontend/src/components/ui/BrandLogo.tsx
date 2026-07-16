import { makeStyles } from "@fluentui/react-components";

const useStyles = makeStyles({
  wrap: {
    display: "flex",
    alignItems: "center",
    gap: "12px",
  },
  img: {
    display: "block",
    objectFit: "contain",
    flexShrink: 0,
  },
  textCol: {
    display: "grid",
    gap: "2px",
    minWidth: 0,
  },
  title: {
    fontSize: "16px",
    fontWeight: 700,
    color: "#1F2937",
    lineHeight: 1.2,
  },
  subtitle: {
    fontSize: "12px",
    color: "#6B7280",
    lineHeight: 1.2,
  },
});

type BrandLogoProps = {
  size?: number;
  title?: string;
  subtitle?: string | null;
  showText?: boolean;
};

/** Logo oficial Olnatura (public/logo-olnatura.png). */
export default function BrandLogo({
  size = 40,
  title = "Sistema Olnatura",
  subtitle = "QR Suite",
  showText = true,
}: BrandLogoProps) {
  const s = useStyles();
  const src = `${import.meta.env.BASE_URL}logo-olnatura.png`;

  return (
    <div className={s.wrap}>
      <img
        src={src}
        alt="Olnatura"
        className={s.img}
        width={size}
        height={size}
        style={{ width: size, height: size }}
      />
      {showText ? (
        <div className={s.textCol}>
          <div className={s.title}>{title}</div>
          {subtitle ? <div className={s.subtitle}>{subtitle}</div> : null}
        </div>
      ) : null}
    </div>
  );
}
