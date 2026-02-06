package com.reservas.util;

import com.reservas.entity.PlantillaEmailConfig;

/**
 * Generador de plantillas HTML para emails personalizados
 * Soporta 3 diseños: Clásico, Moderno y Minimalista
 */
public class EmailTemplateGenerator {

    /**
     * Genera HTML para recordatorio de cita con plantilla personalizada
     */
    public static String generarRecordatorioCita(
            PlantillaEmailConfig config,
            String nombreCliente,
            String nombreServicio,
            String fechaCita,
            String horaCita,
            String nombreNegocio) {

        PlantillaEmailConfig.TipoDiseno diseno = config != null && config.getDisenoBase() != null
                ? config.getDisenoBase()
                : PlantillaEmailConfig.TipoDiseno.CLASICO;

        return switch (diseno) {
            case MODERNO -> generarRecordatorioModerno(config, nombreCliente, nombreServicio, fechaCita, horaCita, nombreNegocio);
            case MINIMALISTA -> generarRecordatorioMinimalista(config, nombreCliente, nombreServicio, fechaCita, horaCita, nombreNegocio);
            default -> generarRecordatorioClasico(config, nombreCliente, nombreServicio, fechaCita, horaCita, nombreNegocio);
        };
    }

    /**
     * Diseño CLÁSICO - Tradicional y profesional
     */
    private static String generarRecordatorioClasico(
            PlantillaEmailConfig config,
            String nombreCliente,
            String nombreServicio,
            String fechaCita,
            String horaCita,
            String nombreNegocio) {

        String colorPrimario = getColor(config, "primario", "#1E40AF");
        String colorSecundario = getColor(config, "secundario", "#3B82F6");
        String colorFondo = getColor(config, "fondo", "#F3F4F6");
        String logoUrl = config != null ? config.getLogoUrl() : null;
        String mensajeBienvenida = getText(config, "bienvenida", "Te recordamos tu próxima cita");
        String firma = getText(config, "firma", "Equipo de " + nombreNegocio);
        String infoContacto = config != null ? config.getInfoContacto() : null;

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: " + colorFondo + "; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }" +
                ".header { background-color: " + colorPrimario + "; padding: 30px; text-align: center; }" +
                ".header-emoji { font-size: 48px; margin-bottom: 10px; }" +
                ".header h1 { color: #ffffff; margin: 0; font-size: 24px; }" +
                ".content { padding: 40px 30px; }" +
                ".greeting { font-size: 16px; color: #374151; margin-bottom: 20px; }" +
                ".info-box { background-color: #F9FAFB; border-left: 4px solid " + colorSecundario + "; padding: 20px; margin: 20px 0; border-radius: 4px; }" +
                ".info-row { margin: 12px 0; }" +
                ".info-label { font-weight: 600; color: #374151; display: inline-block; width: 120px; }" +
                ".info-value { color: #1F2937; }" +
                ".footer { background-color: #F9FAFB; padding: 30px; text-align: center; color: #6B7280; font-size: 14px; }" +
                ".footer-firma { font-weight: 600; color: #374151; margin-bottom: 10px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                (logoUrl != null && !logoUrl.isBlank()
                        ? "<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"max-width: 200px; max-height: 80px; margin-bottom: 10px;\">"
                        : "<div class=\"header-emoji\">🔔</div>") +
                "<h1>Recordatorio de Cita</h1>" +
                "</div>" +
                "<div class=\"content\">" +
                "<p class=\"greeting\">" + mensajeBienvenida + "</p>" +
                "<p>Hola <strong>" + nombreCliente + "</strong>,</p>" +
                "<p>Este es un recordatorio de tu cita programada:</p>" +
                "<div class=\"info-box\">" +
                "<div class=\"info-row\"><span class=\"info-label\">📋 Servicio:</span><span class=\"info-value\">" + nombreServicio + "</span></div>" +
                "<div class=\"info-row\"><span class=\"info-label\">📅 Fecha:</span><span class=\"info-value\">" + fechaCita + "</span></div>" +
                "<div class=\"info-row\"><span class=\"info-label\">🕐 Hora:</span><span class=\"info-value\">" + horaCita + "</span></div>" +
                "<div class=\"info-row\"><span class=\"info-label\">📍 Lugar:</span><span class=\"info-value\">" + nombreNegocio + "</span></div>" +
                "</div>" +
                "<p style=\"color: #6B7280; margin-top: 30px;\">¡Te esperamos!</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "<p class=\"footer-firma\">" + firma + "</p>" +
                (infoContacto != null && !infoContacto.isBlank() ? "<p>" + infoContacto + "</p>" : "") +
                "<p style=\"font-size: 12px; color: #9CA3AF; margin-top: 20px;\">" +
                "Este es un correo automático generado por " + nombreNegocio + "." +
                "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Diseño MODERNO - Con gradientes y sombras
     */
    private static String generarRecordatorioModerno(
            PlantillaEmailConfig config,
            String nombreCliente,
            String nombreServicio,
            String fechaCita,
            String horaCita,
            String nombreNegocio) {

        String colorPrimario = getColor(config, "primario", "#1E40AF");
        String colorSecundario = getColor(config, "secundario", "#3B82F6");
        String colorFondo = getColor(config, "fondo", "#F3F4F6");
        String logoUrl = config != null ? config.getLogoUrl() : null;
        String mensajeBienvenida = getText(config, "bienvenida", "Te recordamos tu próxima cita");
        String firma = getText(config, "firma", "Equipo de " + nombreNegocio);
        String infoContacto = config != null ? config.getInfoContacto() : null;

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { margin: 0; padding: 20px; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, " + colorFondo + " 0%, #E5E7EB 100%); }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, " + colorPrimario + " 0%, " + colorSecundario + " 100%); padding: 40px 30px; text-align: center; position: relative; }" +
                ".header::after { content: ''; position: absolute; bottom: -20px; left: 0; right: 0; height: 20px; background: #ffffff; border-radius: 20px 20px 0 0; }" +
                ".header-emoji { font-size: 56px; margin-bottom: 15px; filter: drop-shadow(0 4px 6px rgba(0,0,0,0.1)); }" +
                ".header h1 { color: #ffffff; margin: 0; font-size: 26px; font-weight: 700; text-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                ".content { padding: 50px 40px; }" +
                ".greeting { font-size: 18px; color: " + colorPrimario + "; margin-bottom: 25px; font-weight: 600; }" +
                ".info-card { background: linear-gradient(135deg, #F9FAFB 0%, #F3F4F6 100%); border-radius: 12px; padding: 25px; margin: 25px 0; box-shadow: 0 4px 6px rgba(0,0,0,0.05); border: 1px solid #E5E7EB; }" +
                ".info-row { margin: 15px 0; padding: 12px; background-color: #ffffff; border-radius: 8px; display: flex; align-items: center; }" +
                ".info-icon { font-size: 20px; margin-right: 12px; }" +
                ".info-content { flex: 1; }" +
                ".info-label { font-weight: 600; color: #6B7280; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; }" +
                ".info-value { color: #1F2937; font-size: 16px; font-weight: 600; margin-top: 4px; }" +
                ".cta-text { text-align: center; margin-top: 35px; }" +
                ".cta-button { display: inline-block; background: linear-gradient(135deg, " + colorPrimario + " 0%, " + colorSecundario + " 100%); color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 25px; font-weight: 600; box-shadow: 0 4px 15px rgba(0,0,0,0.2); }" +
                ".footer { background: linear-gradient(to bottom, #F9FAFB 0%, #F3F4F6 100%); padding: 35px; text-align: center; color: #6B7280; font-size: 14px; }" +
                ".footer-firma { font-weight: 700; color: #374151; margin-bottom: 12px; font-size: 16px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                (logoUrl != null && !logoUrl.isBlank()
                        ? "<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"max-width: 200px; max-height: 80px; margin-bottom: 15px; filter: drop-shadow(0 4px 6px rgba(0,0,0,0.1));\">"
                        : "<div class=\"header-emoji\">🔔</div>") +
                "<h1>Recordatorio de Cita</h1>" +
                "</div>" +
                "<div class=\"content\">" +
                "<p class=\"greeting\">" + mensajeBienvenida + "</p>" +
                "<p style=\"font-size: 16px; color: #374151; line-height: 1.6;\">Hola <strong>" + nombreCliente + "</strong>,</p>" +
                "<p style=\"font-size: 16px; color: #6B7280; line-height: 1.6;\">Queremos recordarte que tienes una cita programada:</p>" +
                "<div class=\"info-card\">" +
                "<div class=\"info-row\"><span class=\"info-icon\">📋</span><div class=\"info-content\"><div class=\"info-label\">Servicio</div><div class=\"info-value\">" + nombreServicio + "</div></div></div>" +
                "<div class=\"info-row\"><span class=\"info-icon\">📅</span><div class=\"info-content\"><div class=\"info-label\">Fecha</div><div class=\"info-value\">" + fechaCita + "</div></div></div>" +
                "<div class=\"info-row\"><span class=\"info-icon\">🕐</span><div class=\"info-content\"><div class=\"info-label\">Hora</div><div class=\"info-value\">" + horaCita + "</div></div></div>" +
                "<div class=\"info-row\"><span class=\"info-icon\">📍</span><div class=\"info-content\"><div class=\"info-label\">Lugar</div><div class=\"info-value\">" + nombreNegocio + "</div></div></div>" +
                "</div>" +
                "<div class=\"cta-text\">" +
                "<p style=\"color: #6B7280; margin-bottom: 15px;\">¡Te esperamos!</p>" +
                "</div>" +
                "</div>" +
                "<div class=\"footer\">" +
                "<p class=\"footer-firma\">" + firma + "</p>" +
                (infoContacto != null && !infoContacto.isBlank() ? "<p style=\"margin: 10px 0;\">" + infoContacto + "</p>" : "") +
                "<p style=\"font-size: 12px; color: #9CA3AF; margin-top: 20px;\">" +
                "Este es un correo automático generado por " + nombreNegocio + "." +
                "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Diseño MINIMALISTA - Simple y elegante
     */
    private static String generarRecordatorioMinimalista(
            PlantillaEmailConfig config,
            String nombreCliente,
            String nombreServicio,
            String fechaCita,
            String horaCita,
            String nombreNegocio) {

        String colorPrimario = getColor(config, "primario", "#000000");
        String colorSecundario = getColor(config, "secundario", "#666666");
        String logoUrl = config != null ? config.getLogoUrl() : null;
        String mensajeBienvenida = getText(config, "bienvenida", "Recordatorio de cita");
        String firma = getText(config, "firma", nombreNegocio);
        String infoContacto = config != null ? config.getInfoContacto() : null;

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { margin: 0; padding: 40px 20px; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #ffffff; }" +
                ".container { max-width: 500px; margin: 0 auto; }" +
                ".header { text-align: center; padding-bottom: 30px; border-bottom: 1px solid #E5E7EB; }" +
                ".logo { max-width: 150px; max-height: 60px; margin-bottom: 20px; }" +
                ".header h1 { color: " + colorPrimario + "; margin: 0; font-size: 18px; font-weight: 400; letter-spacing: 1px; text-transform: uppercase; }" +
                ".content { padding: 40px 0; }" +
                ".greeting { font-size: 14px; color: " + colorSecundario + "; margin-bottom: 30px; text-align: center; letter-spacing: 0.5px; }" +
                ".client-name { font-size: 16px; color: " + colorPrimario + "; margin-bottom: 30px; }" +
                ".info-table { width: 100%; border-collapse: collapse; margin: 30px 0; }" +
                ".info-table td { padding: 15px 0; border-bottom: 1px solid #F3F4F6; }" +
                ".info-table td:first-child { color: " + colorSecundario + "; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; width: 100px; }" +
                ".info-table td:last-child { color: " + colorPrimario + "; font-size: 15px; text-align: right; }" +
                ".footer { padding-top: 40px; border-top: 1px solid #E5E7EB; text-align: center; }" +
                ".footer-firma { color: " + colorPrimario + "; font-size: 14px; font-weight: 500; margin-bottom: 15px; }" +
                ".footer-info { color: " + colorSecundario + "; font-size: 12px; line-height: 1.6; }" +
                ".footer-legal { color: #9CA3AF; font-size: 10px; margin-top: 30px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                (logoUrl != null && !logoUrl.isBlank()
                        ? "<img src=\"" + logoUrl + "\" alt=\"Logo\" class=\"logo\">"
                        : "") +
                "<h1>" + mensajeBienvenida + "</h1>" +
                "</div>" +
                "<div class=\"content\">" +
                "<p class=\"client-name\">" + nombreCliente + "</p>" +
                "<table class=\"info-table\">" +
                "<tr><td>Servicio</td><td>" + nombreServicio + "</td></tr>" +
                "<tr><td>Fecha</td><td>" + fechaCita + "</td></tr>" +
                "<tr><td>Hora</td><td>" + horaCita + "</td></tr>" +
                "<tr><td>Lugar</td><td>" + nombreNegocio + "</td></tr>" +
                "</table>" +
                "</div>" +
                "<div class=\"footer\">" +
                "<p class=\"footer-firma\">" + firma + "</p>" +
                (infoContacto != null && !infoContacto.isBlank() ? "<p class=\"footer-info\">" + infoContacto + "</p>" : "") +
                "<p class=\"footer-legal\">Correo automático · " + nombreNegocio + "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Genera HTML para email de verificación
     */
    public static String generarEmailVerificacion(
            PlantillaEmailConfig config,
            String nombreUsuario,
            String verificationUrl,
            String nombreNegocio) {

        PlantillaEmailConfig.TipoDiseno diseno = config != null && config.getDisenoBase() != null
                ? config.getDisenoBase()
                : PlantillaEmailConfig.TipoDiseno.CLASICO;

        String colorPrimario = getColor(config, "primario", "#1E40AF");
        String colorSecundario = getColor(config, "secundario", "#3B82F6");
        String colorFondo = getColor(config, "fondo", "#F3F4F6");
        String logoUrl = config != null ? config.getLogoUrl() : null;

        // Para verificación, usar diseño más simple
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: " + colorFondo + "; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }" +
                ".header { background-color: " + colorPrimario + "; padding: 30px; text-align: center; }" +
                ".header-emoji { font-size: 48px; margin-bottom: 10px; }" +
                ".header h1 { color: #ffffff; margin: 0; font-size: 24px; }" +
                ".content { padding: 40px 30px; text-align: center; }" +
                ".btn { display: inline-block; background-color: " + colorSecundario + "; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-weight: 600; margin: 20px 0; }" +
                ".btn:hover { opacity: 0.9; }" +
                ".footer { background-color: #F9FAFB; padding: 30px; text-align: center; color: #6B7280; font-size: 14px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                (logoUrl != null && !logoUrl.isBlank()
                        ? "<img src=\"" + logoUrl + "\" alt=\"Logo\" style=\"max-width: 200px; max-height: 80px; margin-bottom: 10px;\">"
                        : "<div class=\"header-emoji\">✨</div>") +
                "<h1>¡Bienvenido a Cita Click!</h1>" +
                "</div>" +
                "<div class=\"content\">" +
                "<p style=\"font-size: 18px; color: #374151;\">Hola <strong>" + nombreUsuario + "</strong>,</p>" +
                "<p style=\"color: #6B7280; margin: 20px 0;\">Gracias por registrarte. Por favor verifica tu correo electrónico haciendo clic en el botón de abajo:</p>" +
                "<a href=\"" + verificationUrl + "\" class=\"btn\">Verificar Email</a>" +
                "<p style=\"color: #9CA3AF; font-size: 14px; margin-top: 30px;\">Este enlace expirará en 24 horas</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "<p style=\"font-size: 12px; color: #9CA3AF;\">Si no creaste esta cuenta, puedes ignorar este mensaje.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    // Métodos auxiliares
    private static String getColor(PlantillaEmailConfig config, String tipo, String defaultColor) {
        if (config == null) return defaultColor;
        return switch (tipo) {
            case "primario" -> config.getColorPrimario() != null ? config.getColorPrimario() : defaultColor;
            case "secundario" -> config.getColorSecundario() != null ? config.getColorSecundario() : defaultColor;
            case "fondo" -> config.getColorFondo() != null ? config.getColorFondo() : defaultColor;
            default -> defaultColor;
        };
    }

    private static String getText(PlantillaEmailConfig config, String tipo, String defaultText) {
        if (config == null) return defaultText;
        return switch (tipo) {
            case "bienvenida" -> config.getMensajeBienvenida() != null && !config.getMensajeBienvenida().isBlank()
                    ? config.getMensajeBienvenida() : defaultText;
            case "firma" -> config.getFirma() != null && !config.getFirma().isBlank()
                    ? config.getFirma() : defaultText;
            default -> defaultText;
        };
    }
}
