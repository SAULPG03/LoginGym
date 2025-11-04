package com.saul.loginGym.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import com.saul.loginGym.dto.ClaseObjetivo;

import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class LoginGymService {

    public void loginTrainingGym() {
        System.out.println("🔹 Iniciando login automático (multi-reserva simple)...");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-features=IsolateOrigins,site-per-process");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        options.setExperimentalOption("prefs", prefs);
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://www.trainingymapp.com/webtouch");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // Login
            WebElement userField = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[ng-model='user']")));
            WebElement passField = driver.findElement(By.cssSelector("input[ng-model='pass']"));

            userField.sendKeys("saulpg03");
            passField.sendKeys("saulpg143");

            WebElement loginButton = driver.findElement(By.cssSelector(".btn-entrar"));
            loginButton.click();
            System.out.println("✅ Login enviado, esperando posible encuesta...");

            manejarEncuesta(driver);

            Thread.sleep(4000);
            var iframes = driver.findElements(By.tagName("iframe"));
            if (!iframes.isEmpty()) {
                driver.switchTo().frame(iframes.get(0));
                System.out.println("➡️ Cambiado al primer iframe.");
            }

            entrarEnActividades(driver);

            // Espera extra para que WebSocket/UI termine de ponerse a tono
            System.out.println("⏳ Esperando conexión WebSocket...");
            Thread.sleep(5000);

            // ====== Aquí tu lista de clases a reservar (añade o quita) ======
            List<ClaseObjetivo> lote = List.of(
                new ClaseObjetivo("MIÉRCOLES", "POWER VIRTUAL", "07:00 / 08:00")
                , new ClaseObjetivo("MIÉRCOLES", "HIIT 30'", "10:30 / 11:00")
                // , new ClaseObjetivo("JUEVES", "CICLO", "19:00 / 20:00")
            );

            reservarMultiplesClases(driver, lote);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("⏸️ Navegador dejado abierto para depuración.");
            // driver.quit();
        }
    }

    // ====== NUEVO: multi-reserva simple, sin prioridades ni reintentos ======
    private void reservarMultiplesClases(WebDriver driver, List<ClaseObjetivo> lote) throws InterruptedException {
        for (ClaseObjetivo c : lote) {
            try {
                System.out.printf("%n🏁 Reservando → %s | %s | %s%n", c.dia, c.nombre, c.rangoHora);

                // asegurar que seguimos en el iframe y en Actividades
                reentrarEnIframeSiHaceFalta(driver);
                asegurarSeccionActividades(driver);

                // flujo de reserva (igual que antes)
                clickClasePorNombreYHora(driver, c.nombre, c.rangoHora, c.dia);
                // manejarReserva(driver) ya se llama dentro de clickClasePorNombreYHora()

                // pequeña pausa entre reservas
                Thread.sleep(1500);

            } catch (Exception e) {
                System.out.println("⚠️ Fallo al reservar " + c.nombre + ": " + e.getMessage());
            }
        }
    }

    private void reentrarEnIframeSiHaceFalta(WebDriver driver) {
        try {
            // si perdimos el frame, volvemos a entrar
            if (driver.findElements(By.id("scrollCalendar")).isEmpty()) {
                var iframes = driver.findElements(By.tagName("iframe"));
                if (!iframes.isEmpty()) {
                    driver.switchTo().defaultContent();
                    driver.switchTo().frame(iframes.get(0));
                    System.out.println("🔁 Re-entrado al iframe principal.");
                }
            }
        } catch (Exception ignored) {}
    }

    private void asegurarSeccionActividades(WebDriver driver) throws InterruptedException {
        try {
            if (driver.findElements(By.id("scrollCalendar")).isEmpty()) {
                System.out.println("🔁 Re-entrando en 'Actividades'…");
                entrarEnActividades(driver);
                Thread.sleep(800);
            } else {
                limpiarOverlays(driver);
            }
        } catch (Exception e) {
            entrarEnActividades(driver);
            Thread.sleep(800);
        }
    }

    // ======================================
    // 🔹 MÉTODO PARA MANEJAR LA RESERVA (CON O SIN PLAZA)
    // ======================================
    private void manejarReserva(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            System.out.println("🎫 Esperando modal de reserva...");
            Thread.sleep(2000);

            debugModal(driver);

            boolean tieneSeleccionPlaza = esModalConPlazas(driver);

            if (tieneSeleccionPlaza) {
                System.out.println("🪑 Modal CON selección de plaza detectado");
                reservarConPlaza(driver, wait);
            } else {
                System.out.println("✅ Modal SIN selección de plaza detectado");
                reservarDirecta(driver, wait);
            }

            System.out.println("✅ Reserva completada exitosamente!");

        } catch (Exception e) {
            System.err.println("⚠️ ERROR en reserva: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void debugModal(WebDriver driver) {
        try {
            System.out.println("\n🔍 DEBUG MODAL:");
            List<WebElement> botones = driver.findElements(
                By.xpath("//div[contains(@class,'btn-tg')]")
            );
            for (int i = 0; i < botones.size(); i++) {
                if (botones.get(i).isDisplayed()) {
                    String texto = botones.get(i).getText();
                    String ngClick = botones.get(i).getAttribute("ng-click");
                    System.out.println("  Botón " + (i+1) + ": '" + texto + "' [" + ngClick + "]");
                }
            }
            List<WebElement> tabla = driver.findElements(By.id("puestos-horario"));
            System.out.println("  Tabla puestos: " + (!tabla.isEmpty() ? "SÍ" : "NO"));
            System.out.println("🔍 FIN DEBUG\n");
        } catch (Exception e) {
            System.err.println("Error en debug: " + e.getMessage());
        }
    }

    private boolean esModalConPlazas(WebDriver driver) {
        try {
            System.out.println("🔍 Detectando tipo de modal...");
            List<WebElement> tablaPuestos = driver.findElements(
                By.xpath("//div[@id='puestos-horario' and contains(@class,'col-xs-12')]")
            );
            if (!tablaPuestos.isEmpty() && tablaPuestos.get(0).isDisplayed()) {
                System.out.println("✅ Tabla de puestos encontrada → Modal CON plazas");
                return true;
            }
            List<WebElement> botonSeleccionarPlaza = driver.findElements(
                By.xpath("//div[@ng-click='actionSelectPlace()']")
            );
            if (!botonSeleccionarPlaza.isEmpty() && botonSeleccionarPlaza.get(0).isDisplayed()) {
                System.out.println("✅ Botón 'actionSelectPlace()' encontrado → Modal CON plazas");
                return true;
            }
            try {
                WebElement modalBody = driver.findElement(
                    By.xpath("//div[contains(@class,'modal-body') and contains(@class,'tg-centrado')]")
                );
                String textoModal = modalBody.getText().toLowerCase();
                if (textoModal.contains("seleccione su plaza") || textoModal.contains("seleccione tu plaza")) {
                    System.out.println("✅ Texto 'seleccione su plaza' encontrado → Modal CON plazas");
                    return true;
                }
            } catch (Exception ignored) {}
            List<WebElement> botonReservarDirecto = driver.findElements(
                By.xpath("//div[@ng-click=\"setDataBook(selectedSchedule, undefined)\"]")
            );
            if (!botonReservarDirecto.isEmpty() && botonReservarDirecto.get(0).isDisplayed()) {
                System.out.println("✅ Botón 'setDataBook' encontrado → Modal SIN plazas");
                return false;
            }
            System.out.println("⚠️ No se pudo determinar con certeza → Asumiendo SIN plazas");
            return false;

        } catch (Exception e) {
            System.err.println("⚠️ Error al detectar tipo de modal: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void reservarConPlaza(WebDriver driver, WebDriverWait wait) {
        try {
            System.out.println("🪑 Buscando plaza disponible...");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("puestos-horario")));
            Thread.sleep(1000);

            List<WebElement> puestosLibres = driver.findElements(
                By.xpath("//div[contains(@class,'puesto_libre') and contains(@class,'puesto')]")
            );
            if (puestosLibres.isEmpty()) {
                throw new Exception("No hay plazas disponibles");
            }

            WebElement primerPuestoLibre = puestosLibres.get(0);
            String numeroPuesto = primerPuestoLibre.findElement(By.className("label")).getText();

            System.out.println("🎯 Seleccionando puesto: " + numeroPuesto);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", primerPuestoLibre);

            Thread.sleep(1000);

            WebElement btnReservar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@ng-click='actionSelectPlace()']//span[contains(text(),'reservar')]/..")
            ));
            System.out.println("✅ Confirmando reserva del puesto " + numeroPuesto);
            js.executeScript("arguments[0].click();", btnReservar);

            Thread.sleep(2000);

        } catch (Exception e) {
            System.err.println("⚠️ ERROR al reservar con plaza: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void reservarDirecta(WebDriver driver, WebDriverWait wait) {
        try {
            System.out.println("✅ Reservando directamente (sin plaza)...");
            try {
                WebElement btnReservar = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@ng-click=\"setDataBook(selectedSchedule, undefined)\" and contains(@class,'btn-tg')]")
                ));
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].scrollIntoView(true);", btnReservar);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", btnReservar);
                System.out.println("✅ Clic en botón 'reservar' (método 1) ejecutado");
                Thread.sleep(2000);
                return;
            } catch (Exception e1) {
                System.err.println("⚠️ Método 1 falló, intentando método 2...");
            }

            try {
                WebElement btnReservar = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'btn-tg-modal-actividad')]//span[contains(translate(text(),'RESERVA','reserva'),'reservar')]/..")
                ));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnReservar);
                System.out.println("✅ Clic en botón 'reservar' (método 2) ejecutado");
                Thread.sleep(2000);
                return;
            } catch (Exception e2) {
                System.err.println("⚠️ Método 2 falló, intentando método 3...");
            }

            List<WebElement> botonesReservar = driver.findElements(
                By.xpath("//div[contains(@class,'btn-tg')]//span[contains(text(),'reservar') or contains(text(),'RESERVAR')]/..")
            );
            for (WebElement btn : botonesReservar) {
                if (btn.isDisplayed() && btn.isEnabled()) {
                    String ngClick = btn.getAttribute("ng-click");
                    if (ngClick != null && !ngClick.contains("actionSelectPlace")) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                        System.out.println("✅ Clic en botón 'reservar' (método 3) ejecutado");
                        Thread.sleep(2000);
                        return;
                    }
                }
            }
            throw new Exception("No se encontró el botón de reservar");

        } catch (Exception e) {
            System.err.println("⚠️ ERROR al reservar directa: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // ======================================
    // 🔹 Encuesta (si aparece)
    // ======================================
    private void manejarEncuesta(WebDriver driver) {
        try {
            WebDriverWait waitModal = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement modalTitulo = waitModal.until(ExpectedConditions
                    .presenceOfElementLocated(By.xpath("//span[contains(text(),'Valora tu experiencia')]")));

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
            WebElement actividadesBtn = waitMenu.until(ExpectedConditions
                    .visibilityOfElementLocated(By.xpath("//span[contains(text(),'Actividades')]/ancestor::li")));

            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", actividadesBtn);
            System.out.println("✅ Clic en 'Actividades' realizado.");
            Thread.sleep(4000);

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

            WebElement tituloAtencion = wait.until(ExpectedConditions.presenceOfElementLocated(By
                    .xpath("//span[@id='mTitulo' and contains(translate(text(),'ATENCIÓN','atención'),'atención')]")));

            if (tituloAtencion != null) {
                System.out.println("⚠️ Modal de atención detectado: " + tituloAtencion.getText());

                List<WebElement> botonesSalir = driver.findElements(By.xpath(
                        "//div[contains(@class,'btn-tg-modal-salir')] | //div[@ng-click='closeActivityModal()']"));

                if (!botonesSalir.isEmpty()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botonesSalir.get(0));
                    System.out.println("✅ Botón 'Salir' pulsado.");
                }

                ((JavascriptExecutor) driver).executeScript(
                        "document.querySelectorAll('.modal, .modal-backdrop').forEach(el => el.remove());"
                                + "document.body.classList.remove('modal-open');");
                System.out.println("🧹 Modal y backdrop eliminados manualmente.");

                Thread.sleep(1000);
                return true;
            }
        } catch (Exception e) { }
        return false;
    }

    // ======================================
    // 🔹 Click específico para AngularJS
    // ======================================
    private void clickClasePorNombreYHora(WebDriver driver, String nombreActividad, String rangoHora, String dia) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            System.out.println("🔍 Buscando clase: " + nombreActividad + " (" + rangoHora + ") el día " + dia);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("scrollCalendar")));
            Thread.sleep(2000);

            limpiarOverlays(driver);

            String diaAbreviado = obtenerAbreviaturaDia(dia);
            System.out.println("📅 Buscando columna del día: " + diaAbreviado);

            List<WebElement> cabecerasDias = driver.findElements(
                By.xpath("//div[contains(@class,'contenedor-cabecera-dias')]//span[@class='label color7 ng-binding']")
            );

            int indiceDia = -1;
            for (int i = 0; i < cabecerasDias.size(); i++) {
                String textoDia = cabecerasDias.get(i).getText().toLowerCase();
                System.out.println("  Día " + i + ": " + textoDia);
                if (textoDia.startsWith(diaAbreviado.toLowerCase())) {
                    indiceDia = i;
                    System.out.println("✅ Columna encontrada en índice: " + i);
                    break;
                }
            }
            if (indiceDia == -1) throw new Exception("No se encontró la columna del día " + dia);

            List<WebElement> columnasDias = driver.findElements(
                By.xpath("//div[contains(@class,'contenedor-item-dia')]")
            );
            if (indiceDia >= columnasDias.size()) throw new Exception("Índice de columna fuera de rango");

            WebElement columnaDia = columnasDias.get(indiceDia);
            System.out.println("📊 Columna del día obtenida correctamente");

            String nombreLower = nombreActividad.toLowerCase(Locale.ROOT);
            List<WebElement> itemsActividades = columnaDia.findElements(
                By.xpath(".//div[contains(@class,'item-dias') and contains(@class,'alturaActividadesReservas')]")
            );
            System.out.println("📊 Encontradas " + itemsActividades.size() + " actividades en " + dia);

            WebElement claseObjetivo = null;
            for (WebElement item : itemsActividades) {
                try {
                    String textoCompleto = item.getText().toLowerCase(Locale.ROOT);
                    if (textoCompleto.contains(nombreLower) && textoCompleto.contains(rangoHora.toLowerCase())) {
                        claseObjetivo = item;
                        System.out.println("✅ Clase encontrada en " + dia + "!");
                        break;
                    }
                } catch (Exception ignored) {}
            }
            if (claseObjetivo == null) throw new Exception("No se encontró la clase " + nombreActividad + " en " + dia);

            realizarClickAngularJS(driver, claseObjetivo, nombreActividad, rangoHora);
            manejarReserva(driver);

        } catch (Exception e) {
            System.err.println("⚠️ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String obtenerAbreviaturaDia(String dia) {
        switch (dia.toUpperCase()) {
            case "LUNES": return "LUN.";
            case "MARTES": return "MAR.";
            case "MIÉRCOLES":
            case "MIERCOLES": return "MIÉ.";
            case "JUEVES": return "JUE.";
            case "VIERNES": return "VIE.";
            case "SÁBADO":
            case "SABADO": return "SÁB.";
            case "DOMINGO": return "DOM.";
            default: throw new IllegalArgumentException("Día no válido: " + dia);
        }
    }

    private void realizarClickAngularJS(WebDriver driver, WebElement elemento, String nombre, String hora)
            throws InterruptedException {

        System.out.println("🎯 Intentando hacer clic en AngularJS...");
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", elemento);
        Thread.sleep(1000);

        boolean clickExitoso = false;

        try {
            Object analisis = ((JavascriptExecutor) driver).executeScript(
                "var el = arguments[0];"
              + "var result = {};"
              + "result.hasAngular = (typeof angular !== 'undefined');"
              + "if (result.hasAngular) {"
              + "  var scope = angular.element(el).scope();"
              + "  result.hasScope = !!scope;"
              + "  result.hasSchedule = scope && !!scope.schedule;"
              + "  result.ngClick = el.getAttribute('ng-click');"
              + "}"
              + "return JSON.stringify(result);", elemento);
            System.out.println("📊 Análisis: " + analisis);
        } catch (Exception e) {
            System.out.println("⚠️ Error en análisis: " + e.getMessage());
        }

        try {
            System.out.println("\n🔄 Estrategia 1: Ejecutando actionBookSchedule...");
            Object resultado = ((JavascriptExecutor) driver).executeScript(
                "var el = arguments[0];"
              + "var scope = angular.element(el).scope();"
              + "if (!scope || !scope.schedule) return 'NO_SCHEDULE';"
              + "var targetScope = scope; var level = 0;"
              + "while (targetScope && level < 10) {"
              + "  if (typeof targetScope.actionBookSchedule === 'function') {"
              + "    targetScope.$apply(function(){ targetScope.actionBookSchedule(scope.schedule); });"
              + "    return 'SUCCESS_AT_LEVEL_' + level;"
              + "  }"
              + "  targetScope = targetScope.$parent; level++; }"
              + "return 'FUNCTION_NOT_FOUND';", elemento);
            System.out.println("📋 Resultado: " + resultado);
            if (resultado != null && resultado.toString().contains("SUCCESS")) {
                clickExitoso = true;
                System.out.println("✅ Función ejecutada correctamente");
                Thread.sleep(3000);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Estrategia 1 falló: " + e.getMessage());
        }

        if (!clickExitoso) {
            try {
                System.out.println("\n🔄 Estrategia 2: Buscando elemento con ng-click...");
                WebElement divNgClick = elemento;
                try {
                    divNgClick = elemento.findElement(By.xpath("./ancestor-or-self::*[@ng-click][1]"));
                    System.out.println("✅ Encontrado elemento con ng-click");
                } catch (Exception ignored) {}
                limpiarOverlays(driver);
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block: 'center'});", divNgClick);
                Thread.sleep(500);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", divNgClick);
                System.out.println("✅ Click JavaScript ejecutado");
                Thread.sleep(3000);
                clickExitoso = true;
            } catch (Exception e) {
                System.out.println("⚠️ Estrategia 2 falló: " + e.getMessage());
            }
        }

        if (!clickExitoso) {
            try {
                System.out.println("\n🔄 Estrategia 3: Simulando eventos de mouse...");
                limpiarOverlays(driver);
                ((JavascriptExecutor) driver).executeScript(
                    "var el = arguments[0]; var evts=['mousedown','mouseup','click'];"
                  + "evts.forEach(function(t){el.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,view:window}))});",
                    elemento);
                System.out.println("✅ Eventos disparados");
                Thread.sleep(3000);
                clickExitoso = true;
            } catch (Exception e) {
                System.out.println("⚠️ Estrategia 3 falló: " + e.getMessage());
            }
        }

        if (!clickExitoso) {
            try {
                System.out.println("\n🔄 Estrategia 4: Click físico con Actions...");
                limpiarOverlays(driver);
                Thread.sleep(500);
                new Actions(driver).moveToElement(elemento).pause(Duration.ofMillis(300)).click().perform();
                System.out.println("✅ Click físico realizado");
                Thread.sleep(3000);
                clickExitoso = true;
            } catch (Exception e) {
                System.out.println("⚠️ Estrategia 4 falló: " + e.getMessage());
            }
        }

        System.out.println("\n🏋️ Verificando resultado...");
        verificarModalReservaDetallado(driver);

        if (!clickExitoso) {
            System.err.println("❌ Ninguna estrategia funcionó");
        }
    }

    private void verificarModalReservaDetallado(WebDriver driver) {
        try {
            System.out.println("🔍 Esperando respuesta de la reserva...");
            Thread.sleep(5000);

            List<WebElement> modales = driver.findElements(
                By.xpath("//div[contains(@class,'modal') or @role='dialog']"));
            System.out.println("📊 Modales encontrados: " + modales.size());

            boolean modalEncontrado = false;
            for (int i = 0; i < modales.size(); i++) {
                WebElement modal = modales.get(i);
                String display = modal.getCssValue("display");
                String visibility = modal.getCssValue("visibility");

                if (!"none".equals(display) && !"hidden".equals(visibility)) {
                    modalEncontrado = true;
                    System.out.println("\n✅ MODAL VISIBLE " + (i+1) + ":");
                    String texto = modal.getText();
                    System.out.println("Texto completo del modal:");
                    System.out.println(texto);

                    List<WebElement> botones = modal.findElements(
                        By.xpath(".//button | .//div[contains(@class,'btn')] | .//a[contains(@class,'btn')]")
                    );
                    System.out.println("\n🔘 Botones en modal: " + botones.size());
                    for (int j = 0; j < botones.size(); j++) {
                        try {
                            String textoBoton = botones.get(j).getText();
                            String ngClick = botones.get(j).getAttribute("ng-click");
                            System.out.println("  Botón " + (j+1) + ": '" + textoBoton + "'" +
                                    (ngClick != null ? " [ng-click=" + ngClick + "]" : ""));
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (!modalEncontrado) {
                System.out.println("⚠️ No se encontró modal visible");
                System.out.println("\n🔍 Buscando cambios en la interfaz...");
                List<WebElement> estadosReserva = driver.findElements(
                    By.xpath("//*[contains(text(),'Reservada') or contains(text(),'reservada') or " +
                             "contains(text(),'Confirmada') or contains(text(),'confirmada') or " +
                             "contains(text(),'En lista de espera') or contains(text(),'lista espera')]"));
                if (!estadosReserva.isEmpty()) {
                    System.out.println("✅ Indicadores de reserva detectados: " + estadosReserva.size());
                } else {
                    System.out.println("❌ No se detectaron cambios de estado");
                }
            }

            imprimirLogsConsola(driver);

        } catch (Exception e) {
            System.out.println("⚠️ Error al verificar: " + e.getMessage());
        }
    }

    private void imprimirLogsConsola(WebDriver driver) {
        try {
            System.out.println("\n📋 LOGS DE CONSOLA:");
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            boolean hayLogs = false;
            for (LogEntry entry : logs) {
                hayLogs = true;
                System.out.println("  " + entry.getLevel() + ": " + entry.getMessage());
            }
            if (!hayLogs) System.out.println("  (No hay logs)");
        } catch (Exception e) {
            System.out.println("  No se pudieron obtener logs");
        }
    }

    private void verificarModalReserva(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
            System.out.println("🔍 Buscando modal de reserva...");

            List<WebElement> modales = driver.findElements(
                By.xpath("//div[contains(@class,'modal') and contains(@style,'display: block')] | " +
                        "//div[contains(@class,'popup')] | //div[@role='dialog']"));
            if (!modales.isEmpty()) {
                System.out.println("✅ Modal detectado! Contenido:");
                WebElement modal = modales.get(0);
                String textoModal = modal.getText();
                System.out.println(textoModal.substring(0, Math.min(200, textoModal.length())));

                Thread.sleep(1000);
                List<WebElement> botonesConfirmar = modal.findElements(
                    By.xpath(".//button[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'confirmar') or " +
                            "contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'reservar') or " +
                            "contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'aceptar')] | " +
                            ".//div[contains(@ng-click,'confirm') or contains(@ng-click,'book') or contains(@ng-click,'accept')]")
                );
                if (!botonesConfirmar.isEmpty()) {
                    System.out.println("🎯 Botón de confirmación encontrado: " + botonesConfirmar.get(0).getText());
                } else {
                    System.out.println("⚠️ No se encontró botón de confirmación en el modal");
                }
            } else {
                System.out.println("ℹ️ No se detectó ningún modal");
                String paginaActual = driver.getPageSource();
                if (paginaActual.contains("ya reservado") || paginaActual.contains("Ya reservada")) {
                    System.out.println("✅ La clase puede estar ya reservada");
                }
            }

        } catch (Exception e) {
            System.out.println("⚠️ Error al verificar modal: " + e.getMessage());
        }
    }

    private void limpiarOverlays(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "// Remover modales y backdrops"
                  + "document.querySelectorAll('.modal-backdrop, .modal, .overlay, [class*=\"overlay\"]').forEach(el => el.remove());"
                  + "document.body.classList.remove('modal-open');"
                  + "document.body.style.overflow = '';"
                  + "// Bajar z-index de elementos agresivos"
                  + "document.querySelectorAll('*').forEach(el => {"
                  + "  const z = window.getComputedStyle(el).zIndex;"
                  + "  if (z > 1000 && !el.id.includes('calendar') && !el.classList.contains('calendar')) { el.style.zIndex = '1'; }"
                  + "});");
            System.out.println("🧹 Overlays limpiados");
        } catch (Exception ignored) {}
    }

    private void guardarDebugInfo(WebDriver driver) {
        try {
            WebElement calendario = driver.findElement(By.id("scrollCalendar"));
            String htmlCalendario = calendario.getAttribute("outerHTML");
            System.out.println("\n📋 HTML DEL CALENDARIO (primeros 500 caracteres):");
            System.out.println(htmlCalendario.substring(0, Math.min(500, htmlCalendario.length())));
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo guardar info de debug: " + e.getMessage());
        }
    }

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
