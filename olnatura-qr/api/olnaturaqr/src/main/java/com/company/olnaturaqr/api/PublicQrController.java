package com.company.olnaturaqr.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qr")
public class PublicQrController {

    @GetMapping(value = "/{lote}", produces = MediaType.TEXT_HTML_VALUE)
    public String landing(@PathVariable String lote) {

        // Importante:
        // - NO consultamos DB
        // - NO consultamos Dynamics
        // - NO mostramos el lote ni nada
        // - Siempre responde lo mismo

        return """
        <!doctype html>
        <html lang="es">
          <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1"/>
            <title>Olnatura QR</title>
            <style>
              body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; margin: 0; background: #f6f7f8; }
              .wrap { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
              .card { max-width: 520px; width: 100%; background: #fff; border: 1px solid #e6e6e6; border-radius: 14px; padding: 18px; }
              h1 { margin: 0 0 8px; font-size: 18px; }
              p { margin: 0; color: #444; line-height: 1.4; }
              .hint { margin-top: 12px; color: #6b6b6b; font-size: 13px; }
              .badge { display:inline-block; padding: 6px 10px; border-radius: 999px; border:1px solid #e6e6e6; background:#fafafa; font-size: 12px; color:#333; margin-bottom: 10px; }
            </style>
          </head>
          <body>
            <div class="wrap">
              <div class="card">
                <div class="badge">QR interno</div>
                <h1>Este código pertenece al sistema interno de Olnatura</h1>
                <p>Para ver información del lote y registrar escaneos, debes usar la app autorizada.</p>
                <p class="hint">Si no tienes acceso, solicita autorización con tu administrador.</p>
              </div>
            </div>
          </body>
        </html>
        """;
    }
}