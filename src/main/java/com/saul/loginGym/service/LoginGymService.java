package com.saul.loginGym.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class LoginGymService {

    public void loginTrainingGym() {
        System.out.println("🔹 Iniciando login automático...");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new");
        options.addArguments("--start-maximized");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://www.trainingymapp.com/webtouch");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // 🟢 Login
            WebElement userField = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[ng-model='user']")));
            WebElement passField = driver.findElement(By.cssSelector("input[ng-model='pass']"));

            userField.sendKeys("saulpg03");
            passField.sendKeys("saulpg143");

            WebElement loginButton = driver.findElement(By.cssSelector(".btn-entrar"));
            loginButton.click();
            System.out.println("✅ Login enviado, esperando posible encuesta...");

            // 🟡 Encuesta (si aparece)
            manejarEncuesta(driver);

            // 🧭 Entrar en Actividades
            Thread.sleep(4000);
            var iframes = driver.findElements(By.tagName("iframe"));
            if (!iframes.isEmpty()) {
                driver.switchTo().frame(iframes.get(0));
                System.out.println("➡️ Cambiado al primer iframe.");
            }

            entrarEnActividades(driver);

            // 🎯 SOLO CLIC: POWER VIRTUAL 13:00 / 14:00
            clickClasePorNombreYHora(driver, "POWER VIRTUAL", "14:30 / 15:30");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("⏸️ Navegador dejado abierto para depuración.");
            // driver.quit();
        }
    }

    // ======================================
    // 🔹 Encuesta (si aparece)
    // ======================================
    private void manejarEncuesta(WebDriver driver) {
        try {
            WebDriverWait waitModal = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement modalTitulo = waitModal.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//span[contains(text(),'Valora tu experiencia')]")));

            if (modalTitulo != null) {
                System.out.println("🟡 Encuesta detectada, gestionando...");

                List<WebElement> respuestas = driver.findElements(By.xpath("//span[text()='5']"));
                if (!respuestas.isEmpty()) {
                    respuestas.get(0).click();
                    System.out.println("✅ Respuesta 5 seleccionada.");
                }

                List<WebElement> enviarBtn = driver.findElements(By.xpath("//div[@ng-click='grabarEncuesta()']"));
                if (!enviarBtn.isEmpty()) {
                    enviarBtn.get(0).click();
                    System.out.println("✅ Encuesta enviada.");
                }

                waitModal.until(ExpectedConditions.invisibilityOf(modalTitulo));
            }

        } catch (Exception e) {
            System.out.println("ℹ️ No apareció encuesta, continuando...");
        }
    }

    // ======================================
    // 🔹 Entrar en Actividades
    // ======================================
    private void entrarEnActividades(WebDriver driver) throws InterruptedException {
        try {
            System.out.println("🎯 Accediendo a 'Actividades'...");

            WebDriverWait waitMenu = new WebDriverWait(driver, Duration.ofSeconds(20));
            WebElement actividadesBtn = waitMenu.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(text(),'Actividades')]/ancestor::li")));

            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", actividadesBtn);
            System.out.println("✅ Clic en 'Actividades' realizado.");
            Thread.sleep(4000);

            // Cerrar modal si aparece
            if (manejarModalAtencion(driver)) {
                System.out.println("⚠️ Se detectó modal de atención, reintentando acceso...");
                Thread.sleep(3000);
            }

            System.out.println("✅ Sección Actividades cargada correctamente.");

        } catch (Exception e) {
            System.err.println("❌ Error al acceder a 'Actividades': " + e.getMessage());
        }
    }

    // ======================================
    // 🔹 Manejar modal “atención”
    // ======================================
    private boolean manejarModalAtencion(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));

            WebElement tituloAtencion = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//span[@id='mTitulo' and contains(translate(text(),'ATENCIÓN','atención'),'atención')]")));

            if (tituloAtencion != null) {
                System.out.println("⚠️ Modal de atención detectado: " + tituloAtencion.getText());

                // Intentar pulsar botón salir o cerrar Angular
                List<WebElement> botonesSalir = driver.findElements(
                        By.xpath("//div[contains(@class,'btn-tg-modal-salir')] | //div[@ng-click='closeActivityModal()']"));

                if (!botonesSalir.isEmpty()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botonesSalir.get(0));
                    System.out.println("✅ Botón 'Salir' pulsado.");
                }

                // Limpieza forzada: eliminar modal + backdrop con JS
                ((JavascriptExecutor) driver).executeScript(
                        "document.querySelectorAll('.modal, .modal-backdrop').forEach(el => el.remove());" +
                        "document.body.classList.remove('modal-open');"
                );
                System.out.println("🧹 Modal y backdrop eliminados manualmente.");

                Thread.sleep(1000);
                return true;
            }
        } catch (Exception e) {
            // No apareció el modal
        }
        return false;
    }

    // ======================================
    // 🔹 SOLO CLIC a un bloque de clase por nombre y hora
    // ======================================
 // ======================================
 // 🔹 CLIC MEJORADO en clase por nombre y hora
 // ======================================
 private void clickClasePorNombreYHora(WebDriver driver, String nombreActividad, String rangoHora) {
     WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
     
     try {
         System.out.println("🔍 Buscando clase: " + nombreActividad + " (" + rangoHora + ")");
         
         // 1. Esperar que el calendario esté completamente cargado
         wait.until(ExpectedConditions.presenceOfElementLocated(By.id("scrollCalendar")));
         Thread.sleep(2000); // Dar tiempo extra para animaciones Angular
         
         // 2. Eliminar cualquier overlay o backdrop residual
         limpiarOverlays(driver);
         
         // 3. Buscar el elemento con XPath simplificado
         String nombreLower = nombreActividad.toLowerCase(Locale.ROOT);
         
         // XPath más robusto - busca por partes
         String xpathBase = "//div[contains(@class,'item-dias')]";
         List<WebElement> itemsDias = driver.findElements(By.xpath(xpathBase));
         
         System.out.println("📊 Encontrados " + itemsDias.size() + " items de actividades");
         
         WebElement claseObjetivo = null;
         
         // 4. Iterar y buscar la clase específica
         for (WebElement item : itemsDias) {
             try {
                 String textoCompleto = item.getText().toLowerCase(Locale.ROOT);
                 
                 // Debug: mostrar qué contiene cada item
                 if (textoCompleto.contains(nombreLower.substring(0, Math.min(5, nombreLower.length())))) {
                     System.out.println("🔍 Item encontrado con '" + nombreActividad + "': " + 
                                      textoCompleto.substring(0, Math.min(100, textoCompleto.length())));
                 }
                 
                 // Verificar si contiene el nombre y la hora
                 if (textoCompleto.contains(nombreLower) && textoCompleto.contains(rangoHora.toLowerCase())) {
                     claseObjetivo = item;
                     System.out.println("✅ Clase encontrada!");
                     break;
                 }
             } catch (Exception e) {
                 // Continuar con el siguiente elemento
             }
         }
         
         if (claseObjetivo == null) {
             System.err.println("❌ No se encontró la clase. Intentando XPath directo...");
             
             // Fallback: XPath más simple
             String xpathSimple = String.format(
                 "//div[contains(@class,'item-dias') and contains(., '%s') and contains(., '%s')]",
                 rangoHora, nombreActividad
             );
             
             claseObjetivo = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathSimple)));
         }
         
         // 5. Hacer clic con múltiples estrategias
         realizarClickRobusto(driver, claseObjetivo, nombreActividad, rangoHora);
         
     } catch (TimeoutException te) {
         System.err.println("⏳ TIMEOUT: No se encontró la clase '" + nombreActividad + 
                          "' (" + rangoHora + ") en el tiempo esperado.");
         
         // Debug: Capturar screenshot o HTML
         guardarDebugInfo(driver);
         
     } catch (Exception e) {
         System.err.println("⚠️ ERROR al hacer clic: " + e.getMessage());
         e.printStackTrace();
     }
 }

 // ======================================
 // 🔹 Realizar clic con múltiples estrategias
 // ======================================
 private void realizarClickRobusto(WebDriver driver, WebElement elemento, String nombre, String hora) 
         throws InterruptedException {
     
     System.out.println("🎯 Intentando hacer clic en la clase...");
     
     // Estrategia 1: Scroll y espera
     ((JavascriptExecutor) driver).executeScript(
         "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
         elemento
     );
     Thread.sleep(1000);
     
     // Estrategia 2: Verificar que sea clickeable
     WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
     wait.until(ExpectedConditions.elementToBeClickable(elemento));
     
     // Estrategia 3: Remover cualquier overlay antes del clic
     limpiarOverlays(driver);
     
     boolean clickExitoso = false;
     
     // Intento 1: Click normal
     try {
         elemento.click();
         clickExitoso = true;
         System.out.println("✅ Click normal exitoso");
     } catch (ElementClickInterceptedException e) {
         System.out.println("⚠️ Click interceptado, intentando con JavaScript...");
     }
     
     // Intento 2: Click con JavaScript
     if (!clickExitoso) {
         try {
             ((JavascriptExecutor) driver).executeScript("arguments[0].click();", elemento);
             clickExitoso = true;
             System.out.println("✅ Click con JavaScript exitoso");
         } catch (Exception e) {
             System.out.println("⚠️ Click JS falló, intentando con Actions...");
         }
     }
     
     // Intento 3: Click con Actions (mover mouse y click)
     if (!clickExitoso) {
         try {
             org.openqa.selenium.interactions.Actions actions = 
                 new org.openqa.selenium.interactions.Actions(driver);
             
             actions.moveToElement(elemento)
                    .pause(Duration.ofMillis(500))
                    .click()
                    .perform();
             
             clickExitoso = true;
             System.out.println("✅ Click con Actions exitoso");
         } catch (Exception e) {
             System.out.println("⚠️ Click con Actions falló");
         }
     }
     
     // Intento 4: Click directo en coordenadas
     if (!clickExitoso) {
         try {
             ((JavascriptExecutor) driver).executeScript(
                 "var element = arguments[0];" +
                 "var event = new MouseEvent('click', {" +
                 "    view: window," +
                 "    bubbles: true," +
                 "    cancelable: true" +
                 "});" +
                 "element.dispatchEvent(event);",
                 elemento
             );
             
             clickExitoso = true;
             System.out.println("✅ Click con evento MouseEvent exitoso");
         } catch (Exception e) {
             System.err.println("❌ Todos los intentos de click fallaron");
         }
     }
     
     if (clickExitoso) {
         Thread.sleep(2000); // Esperar respuesta
         System.out.println("🏋️ Clic en '" + nombre + "' (" + hora + ") completado.");
         
         // Verificar si abrió un modal de reserva
         verificarModalReserva(driver);
     }
 }

 // ======================================
 // 🔹 Limpiar overlays y elementos bloqueantes
 // ======================================
 private void limpiarOverlays(WebDriver driver) {
     try {
         ((JavascriptExecutor) driver).executeScript(
             "// Remover modales y backdrops" +
             "document.querySelectorAll('.modal-backdrop, .modal, .overlay, [class*=\"overlay\"]').forEach(el => el.remove());" +
             "// Remover clase modal-open del body" +
             "document.body.classList.remove('modal-open');" +
             "document.body.style.overflow = '';" +
             "// Remover cualquier elemento con z-index alto que no sea el calendario" +
             "document.querySelectorAll('*').forEach(el => {" +
             "    const zIndex = window.getComputedStyle(el).zIndex;" +
             "    if (zIndex > 1000 && !el.id.includes('calendar') && !el.classList.contains('calendar')) {" +
             "        el.style.zIndex = '1';" +
             "    }" +
             "});"
         );
         System.out.println("🧹 Overlays limpiados");
     } catch (Exception e) {
         // Ignorar si falla
     }
 }

 // ======================================
 // 🔹 Verificar si se abrió modal de reserva
 // ======================================
 private void verificarModalReserva(WebDriver driver) {
     try {
         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
         
         // Buscar modal de reserva o confirmación
         WebElement modal = wait.until(ExpectedConditions.presenceOfElementLocated(
             By.xpath("//div[contains(@class,'modal') or contains(@class,'popup')]" +
                     "[.//span[contains(text(),'Reserva') or contains(text(),'reserva') or " +
                     "contains(text(),'Confirmar') or contains(text(),'confirmar')]]")
         ));
         
         if (modal != null) {
             System.out.println("✅ Modal de reserva detectado!");
             
             // Aquí puedes añadir lógica para confirmar la reserva
             // Por ejemplo, buscar y hacer clic en el botón de confirmar
             Thread.sleep(1000);
             
             List<WebElement> botonesConfirmar = driver.findElements(
                 By.xpath("//button[contains(text(),'Confirmar') or contains(text(),'Reservar') or " +
                         "contains(text(),'Aceptar')] | " +
                         "//div[contains(@ng-click,'confirmar') or contains(@ng-click,'reservar')]")
             );
             
             if (!botonesConfirmar.isEmpty()) {
                 System.out.println("🎯 Botón de confirmación encontrado");
                 // Descomentar para confirmar automáticamente:
                 // botonesConfirmar.get(0).click();
                 // System.out.println("✅ Reserva confirmada!");
             }
         }
         
     } catch (TimeoutException e) {
         System.out.println("ℹ️ No se detectó modal de reserva (puede que ya esté reservada)");
     } catch (Exception e) {
         System.out.println("⚠️ Error al verificar modal: " + e.getMessage());
     }
 }

 // ======================================
 // 🔹 Guardar información de debug
 // ======================================
 private void guardarDebugInfo(WebDriver driver) {
     try {
         // Imprimir el HTML del calendario para debug
         WebElement calendario = driver.findElement(By.id("scrollCalendar"));
         String htmlCalendario = calendario.getAttribute("outerHTML");
         
         System.out.println("\n📋 HTML DEL CALENDARIO (primeros 500 caracteres):");
         System.out.println(htmlCalendario.substring(0, Math.min(500, htmlCalendario.length())));
         
         // Opcional: Guardar screenshot
         // File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
         // Files.copy(screenshot.toPath(), Paths.get("debug_screenshot.png"));
         
     } catch (Exception e) {
         System.err.println("⚠️ No se pudo guardar info de debug: " + e.getMessage());
     }
 }
    // ====== (UTILIDAD EXTRA) Índice de día — por si lo vuelves a usar ======
    private int obtenerIndiceDia(String dia) {
        switch (dia.toLowerCase()) {
            case "lunes": return 0;
            case "martes": return 1;
            case "miércoles":
            case "miercoles": return 2;
            case "jueves": return 3;
            case "viernes": return 4;
            case "sábado":
            case "sabado": return 5;
            case "domingo": return 6;
            default: return -1;
        }
    }
}
