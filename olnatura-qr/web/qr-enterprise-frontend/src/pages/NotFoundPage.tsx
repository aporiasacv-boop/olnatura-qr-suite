import { brand } from "../styles/brand";

export default function NotFoundPage() {
  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: "20px", fontWeight: 600, color: brand.text, margin: 0 }}>404</h1>
      <div style={{ color: brand.muted, marginTop: 8, fontSize: 14 }}>La ruta no existe.</div>
    </div>
  );
}