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
	
	import io.github.bonigarcia.wdm.WebDriverManager;
	
	@Service
	public class LoginGymService {
	
		public void loginTrainingGym() {
		    System.out.println("🔹 Iniciando login automático...");
	
		    WebDriverManager.chromedriver().setup();
	
		    ChromeOptions options = new ChromeOptions();
		    options.addArguments("--start-maximized");
		    
		    // ⚠️ CRÍTICO: Deshabilitar seguridad para permitir WebSocket/CORS
		    options.addArguments("--disable-web-security");
		    options.addArguments("--disable-features=IsolateOrigins,site-per-process");
		    options.addArguments("--allow-running-insecure-content");
		    options.addArguments("--disable-blink-features=AutomationControlled");
		    
		    // Opcional: simular usuario real
		    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36");
		    
		    // Preferencias adicionales
		    Map<String, Object> prefs = new HashMap<>();
		    prefs.put("profile.default_content_setting_values.notifications", 2);
		    options.setExperimentalOption("prefs", prefs);
		    options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
		    options.setExperimentalOption("useAutomationExtension", false);
	
		    WebDriver driver = new ChromeDriver(options);
	
		    try {
		        driver.get("https://www.trainingymapp.com/webtouch");
		        
		        // ... resto del código igual ...
		        
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
	
		        // ⚠️ IMPORTANTE: Esperar más tiempo para que se establezca la conexión WebSocket
		        System.out.println("⏳ Esperando conexión WebSocket...");
		        Thread.sleep(5000);
	
		        clickClasePorNombreYHora(driver, "POWER VIRTUAL", "07:00 / 08:00", "MIÉRCOLES");
	
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
	
				WebElement tituloAtencion = wait.until(ExpectedConditions.presenceOfElementLocated(By
						.xpath("//span[@id='mTitulo' and contains(translate(text(),'ATENCIÓN','atención'),'atención')]")));
	
				if (tituloAtencion != null) {
					System.out.println("⚠️ Modal de atención detectado: " + tituloAtencion.getText());
	
					// Intentar pulsar botón salir o cerrar Angular
					List<WebElement> botonesSalir = driver.findElements(By.xpath(
							"//div[contains(@class,'btn-tg-modal-salir')] | //div[@ng-click='closeActivityModal()']"));
	
					if (!botonesSalir.isEmpty()) {
						((JavascriptExecutor) driver).executeScript("arguments[0].click();", botonesSalir.get(0));
						System.out.println("✅ Botón 'Salir' pulsado.");
					}
	
					// Limpieza forzada: eliminar modal + backdrop con JS
					((JavascriptExecutor) driver).executeScript(
							"document.querySelectorAll('.modal, .modal-backdrop').forEach(el => el.remove());"
									+ "document.body.classList.remove('modal-open');");
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
		// 🔹 CLIC ESPECÍFICO PARA ANGULARJS
		// ======================================
		private void clickClasePorNombreYHora(WebDriver driver, String nombreActividad, String rangoHora, String dia) {
		    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
		    
		    try {
		        System.out.println("🔍 Buscando clase: " + nombreActividad + " (" + rangoHora + ") el día " + dia);
		        
		        // 1. Esperar que el calendario esté cargado
		        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("scrollCalendar")));
		        Thread.sleep(2000);
		        
		        // 2. Limpiar overlays
		        limpiarOverlays(driver);
		        
		        // 3. Obtener la abreviatura del día (ej: MARTES -> "Mar.")
		        String diaAbreviado = obtenerAbreviaturaDia(dia);
		        System.out.println("📅 Buscando columna del día: " + diaAbreviado);
		        
		        // 4. Buscar la columna del día específico en la cabecera
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
		        
		        if (indiceDia == -1) {
		            throw new Exception("No se encontró la columna del día " + dia);
		        }
		        
		        // 5. Buscar todas las columnas de días
		        List<WebElement> columnasDias = driver.findElements(
		            By.xpath("//div[contains(@class,'contenedor-item-dia')]")
		        );
		        
		        if (indiceDia >= columnasDias.size()) {
		            throw new Exception("Índice de columna fuera de rango");
		        }
		        
		        // 6. Obtener la columna específica del día
		        WebElement columnaDia = columnasDias.get(indiceDia);
		        System.out.println("📊 Columna del día obtenida correctamente");
		        
		        // 7. Buscar la actividad dentro de esa columna específica
		        String nombreLower = nombreActividad.toLowerCase(Locale.ROOT);
		        List<WebElement> itemsActividades = columnaDia.findElements(
		            By.xpath(".//div[contains(@class,'item-dias') and contains(@class,'alturaActividadesReservas')]")
		        );
		        
		        System.out.println("📊 Encontradas " + itemsActividades.size() + " actividades en " + dia);
		        
		        WebElement claseObjetivo = null;
		        
		        // 8. Buscar la clase específica dentro de esta columna
		        for (WebElement item : itemsActividades) {
		            try {
		                String textoCompleto = item.getText().toLowerCase(Locale.ROOT);
		                
		                if (textoCompleto.contains(nombreLower.substring(0, Math.min(5, nombreLower.length())))) {
		                    System.out.println("🔍 Item encontrado con '" + nombreActividad + "': " + 
		                                     textoCompleto.substring(0, Math.min(100, textoCompleto.length())));
		                }
		                
		                if (textoCompleto.contains(nombreLower) && textoCompleto.contains(rangoHora.toLowerCase())) {
		                    claseObjetivo = item;
		                    System.out.println("✅ Clase encontrada en " + dia + "!");
		                    break;
		                }
		            } catch (Exception e) {
		                // Continuar
		            }
		        }
		        
		        if (claseObjetivo == null) {
		            throw new Exception("No se encontró la clase " + nombreActividad + " en " + dia);
		        }
		        
		        // 9. HACER CLIC CON ANGULARJS
		        realizarClickAngularJS(driver, claseObjetivo, nombreActividad, rangoHora);
		        
		    } catch (Exception e) {
		        System.err.println("⚠️ ERROR: " + e.getMessage());
		        e.printStackTrace();
		    }
		}
		
		
		// ======================================
		// 🔹 Obtener abreviatura del día con tilde
		// ======================================
		private String obtenerAbreviaturaDia(String dia) {
		    switch (dia.toUpperCase()) {
		        case "LUNES":
		            return "LUN.";
		        case "MARTES":
		            return "MAR.";
		        case "MIÉRCOLES":
		        case "MIERCOLES":
		            return "MIÉ.";
		        case "JUEVES":
		            return "JUE.";
		        case "VIERNES":
		            return "VIE.";
		        case "SÁBADO":
		        case "SABADO":
		            return "SÁB.";
		        case "DOMINGO":
		            return "DOM.";
		        default:
		            throw new IllegalArgumentException("Día no válido: " + dia);
		    }
		}
		
		
		// ======================================
		// 🔹 VERSIÓN SIMPLIFICADA Y FUNCIONAL
		// ======================================
		private void realizarClickAngularJS(WebDriver driver, WebElement elemento, String nombre, String hora) 
		        throws InterruptedException {
		    
		    System.out.println("🎯 Intentando hacer clic en AngularJS...");
		    
		    // Scroll
		    ((JavascriptExecutor) driver).executeScript(
		        "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
		        elemento
		    );
		    Thread.sleep(1000);
		    
		    boolean clickExitoso = false;
		    
		    // ==========================================
		    // ANÁLISIS: Ver qué tenemos disponible
		    // ==========================================
		    System.out.println("\n🔍 ANALIZANDO ELEMENTO...");
		    
		    try {
		        Object analisis = ((JavascriptExecutor) driver).executeScript(
		            "var el = arguments[0];" +
		            "var result = {};" +
		            "result.hasAngular = (typeof angular !== 'undefined');" +
		            "if (result.hasAngular) {" +
		            "  var scope = angular.element(el).scope();" +
		            "  result.hasScope = !!scope;" +
		            "  result.hasSchedule = scope && !!scope.schedule;" +
		            "  result.ngClick = el.getAttribute('ng-click');" +
		            "}" +
		            "return JSON.stringify(result);",
		            elemento
		        );
		        
		        System.out.println("📊 Análisis: " + analisis);
		    } catch (Exception e) {
		        System.out.println("⚠️ Error en análisis: " + e.getMessage());
		    }
		    
		    // ==========================================
		    // ESTRATEGIA 1: Ejecutar actionBookSchedule
		    // ==========================================
		    try {
		        System.out.println("\n🔄 Estrategia 1: Ejecutando actionBookSchedule...");
		        
		        Object resultado = ((JavascriptExecutor) driver).executeScript(
		            "var el = arguments[0];" +
		            "var scope = angular.element(el).scope();" +
		            "if (!scope || !scope.schedule) return 'NO_SCHEDULE';" +
		            "" +
		            "var targetScope = scope;" +
		            "var level = 0;" +
		            "while (targetScope && level < 10) {" +
		            "  if (typeof targetScope.actionBookSchedule === 'function') {" +
		            "    targetScope.$apply(function() {" +
		            "      targetScope.actionBookSchedule(scope.schedule);" +
		            "    });" +
		            "    return 'SUCCESS_AT_LEVEL_' + level;" +
		            "  }" +
		            "  targetScope = targetScope.$parent;" +
		            "  level++;" +
		            "}" +
		            "return 'FUNCTION_NOT_FOUND';",
		            elemento
		        );
		        
		        System.out.println("📋 Resultado: " + resultado);
		        
		        if (resultado != null && resultado.toString().contains("SUCCESS")) {
		            clickExitoso = true;
		            System.out.println("✅ Función ejecutada correctamente");
		            Thread.sleep(3000); // Dar más tiempo
		        }
		        
		    } catch (Exception e) {
		        System.out.println("⚠️ Estrategia 1 falló: " + e.getMessage());
		    }
		    
		    // ==========================================
		    // ESTRATEGIA 2: Buscar y hacer click en elemento ng-click
		    // ==========================================
		    if (!clickExitoso) {
		        try {
		            System.out.println("\n🔄 Estrategia 2: Buscando elemento con ng-click...");
		            
		            // Buscar el div padre con ng-click
		            WebElement divNgClick = elemento;
		            try {
		                divNgClick = elemento.findElement(
		                    By.xpath("./ancestor-or-self::*[@ng-click][1]")
		                );
		                System.out.println("✅ Encontrado elemento con ng-click");
		            } catch (Exception e) {
		                System.out.println("⚠️ Usando elemento actual");
		            }
		            
		            limpiarOverlays(driver);
		            
		            // Hacer scroll y esperar
		            ((JavascriptExecutor) driver).executeScript(
		                "arguments[0].scrollIntoView({block: 'center'});",
		                divNgClick
		            );
		            Thread.sleep(500);
		            
		            // Click con JavaScript
		            ((JavascriptExecutor) driver).executeScript(
		                "arguments[0].click();",
		                divNgClick
		            );
		            
		            System.out.println("✅ Click JavaScript ejecutado");
		            Thread.sleep(3000);
		            
		            clickExitoso = true;
		            
		        } catch (Exception e) {
		            System.out.println("⚠️ Estrategia 2 falló: " + e.getMessage());
		        }
		    }
		    
		    // ==========================================
		    // ESTRATEGIA 3: Simular eventos de mouse
		    // ==========================================
		    if (!clickExitoso) {
		        try {
		            System.out.println("\n🔄 Estrategia 3: Simulando eventos de mouse...");
		            
		            limpiarOverlays(driver);
		            
		            ((JavascriptExecutor) driver).executeScript(
		                "var el = arguments[0];" +
		                "var events = ['mousedown', 'mouseup', 'click'];" +
		                "events.forEach(function(type) {" +
		                "  var evt = new MouseEvent(type, {" +
		                "    bubbles: true," +
		                "    cancelable: true," +
		                "    view: window" +
		                "  });" +
		                "  el.dispatchEvent(evt);" +
		                "});",
		                elemento
		            );
		            
		            System.out.println("✅ Eventos disparados");
		            Thread.sleep(3000);
		            
		            clickExitoso = true;
		            
		        } catch (Exception e) {
		            System.out.println("⚠️ Estrategia 3 falló: " + e.getMessage());
		        }
		    }
		    
		    // ==========================================
		    // ESTRATEGIA 4: Click físico con Actions
		    // ==========================================
		    if (!clickExitoso) {
		        try {
		            System.out.println("\n🔄 Estrategia 4: Click físico con Actions...");
		            
		            limpiarOverlays(driver);
		            Thread.sleep(500);
		            
		            Actions actions = new Actions(driver);
		            actions.moveToElement(elemento)
		                   .pause(Duration.ofMillis(300))
		                   .click()
		                   .perform();
		            
		            System.out.println("✅ Click físico realizado");
		            Thread.sleep(3000);
		            
		            clickExitoso = true;
		            
		        } catch (Exception e) {
		            System.out.println("⚠️ Estrategia 4 falló: " + e.getMessage());
		        }
		    }
		    
		    // ==========================================
		    // Verificar resultado
		    // ==========================================
		    System.out.println("\n🏋️ Verificando resultado...");
		    verificarModalReservaDetallado(driver);
		    
		    if (!clickExitoso) {
		        System.err.println("❌ Ninguna estrategia funcionó");
		    }
		}
	
		// ======================================
		// 🔹 Verificar modal con más detalle
		// ======================================
		private void verificarModalReservaDetallado(WebDriver driver) {
		    try {
		        System.out.println("🔍 Esperando respuesta de la reserva...");
		        
		        // Esperar más tiempo ya que depende de WebSocket
		        Thread.sleep(5000);
		        
		        // 1. Buscar modales
		        List<WebElement> modales = driver.findElements(
		            By.xpath("//div[contains(@class,'modal') or @role='dialog']")
		        );
		        
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
		                
		                // Buscar botones
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
		                        
		                        // ⚠️ DESCOMENTAR PARA AUTO-CONFIRMAR:
		                        // if (textoBoton.toLowerCase().contains("confirmar") || 
		                        //     textoBoton.toLowerCase().contains("aceptar") ||
		                        //     textoBoton.toLowerCase().contains("reservar")) {
		                        //     System.out.println("🎯 Haciendo clic en: " + textoBoton);
		                        //     Thread.sleep(500);
		                        //     ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botones.get(j));
		                        //     System.out.println("✅ Reserva confirmada!");
		                        //     Thread.sleep(2000);
		                        //     break;
		                        // }
		                        
		                    } catch (Exception e) {
		                        // Ignorar
		                    }
		                }
		            }
		        }
		        
		        if (!modalEncontrado) {
		            System.out.println("⚠️ No se encontró modal visible");
		            
		            // Verificar si hay cambios en el elemento de la clase
		            System.out.println("\n🔍 Buscando cambios en la interfaz...");
		            
		            // Buscar si el botón cambió de "Reservar ya" a otra cosa
		            List<WebElement> estadosReserva = driver.findElements(
		                By.xpath("//*[contains(text(),'Reservada') or contains(text(),'reservada') or " +
		                        "contains(text(),'Confirmada') or contains(text(),'confirmada') or " +
		                        "contains(text(),'En lista de espera') or contains(text(),'lista espera')]")
		            );
		            
		            if (!estadosReserva.isEmpty()) {
		                System.out.println("✅ Encontrados " + estadosReserva.size() + " indicadores de reserva:");
		                for (WebElement elem : estadosReserva) {
		                    try {
		                        System.out.println("  - " + elem.getText());
		                    } catch (Exception e) {
		                        // Ignorar
		                    }
		                }
		            } else {
		                System.out.println("❌ No se detectaron cambios de estado");
		            }
		        }
		        
		        // Ver logs actualizados
		        imprimirLogsConsola(driver);
		        
		    } catch (Exception e) {
		        System.out.println("⚠️ Error al verificar: " + e.getMessage());
		    }
		}
		// ======================================
		// 🔹 Imprimir logs de consola
		// ======================================
		private void imprimirLogsConsola(WebDriver driver) {
		    try {
		        System.out.println("\n📋 LOGS DE CONSOLA:");
		        
		        LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
		        
		        boolean hayLogs = false;
		        for (LogEntry entry : logs) {
		            hayLogs = true;
		            System.out.println("  " + entry.getLevel() + ": " + entry.getMessage());
		        }
		        
		        if (!hayLogs) {
		            System.out.println("  (No hay logs)");
		        }
		        
		    } catch (Exception e) {
		        System.out.println("  No se pudieron obtener logs");
		    }
		}
	
	
	
	
		// ======================================
		// 🔹 Verificar modal de reserva (MEJORADO)
		// ======================================
		private void verificarModalReserva(WebDriver driver) {
		    try {
		        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
		        
		        System.out.println("🔍 Buscando modal de reserva...");
		        
		        // Buscar cualquier modal que haya aparecido
		        List<WebElement> modales = driver.findElements(
		            By.xpath("//div[contains(@class,'modal') and contains(@style,'display: block')] | " +
		                    "//div[contains(@class,'popup')] | " +
		                    "//div[@role='dialog']")
		        );
		        
		        if (!modales.isEmpty()) {
		            System.out.println("✅ Modal detectado! Contenido:");
		            WebElement modal = modales.get(0);
		            String textoModal = modal.getText();
		            System.out.println(textoModal.substring(0, Math.min(200, textoModal.length())));
		            
		            // Buscar botones de confirmación
		            Thread.sleep(1000);
		            
		            List<WebElement> botonesConfirmar = modal.findElements(
		                By.xpath(".//button[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'confirmar') or " +
		                        "contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'reservar') or " +
		                        "contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'aceptar')] | " +
		                        ".//div[contains(@ng-click,'confirm') or contains(@ng-click,'book') or contains(@ng-click,'accept')]")
		            );
		            
		            if (!botonesConfirmar.isEmpty()) {
		                System.out.println("🎯 Botón de confirmación encontrado: " + botonesConfirmar.get(0).getText());
		                System.out.println("💡 Para confirmar automáticamente, descomenta la siguiente línea en el código");
		                
		                // ⚠️ DESCOMENTAR PARA CONFIRMAR AUTOMÁTICAMENTE:
		                // Thread.sleep(500);
		                // ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botonesConfirmar.get(0));
		                // System.out.println("✅ Reserva confirmada automáticamente!");
		                
		            } else {
		                System.out.println("⚠️ No se encontró botón de confirmación en el modal");
		            }
		            
		        } else {
		            System.out.println("ℹ️ No se detectó ningún modal");
		            
		            // Verificar si hay algún cambio en la página
		            String paginaActual = driver.getPageSource();
		            if (paginaActual.contains("ya reservado") || paginaActual.contains("Ya reservada")) {
		                System.out.println("✅ La clase puede estar ya reservada");
		            }
		        }
		        
		    } catch (Exception e) {
		        System.out.println("⚠️ Error al verificar modal: " + e.getMessage());
		    }
		}
		
		
		// ======================================
		// 🔹 Limpiar overlays y elementos bloqueantes
		// ======================================
		private void limpiarOverlays(WebDriver driver) {
			try {
				((JavascriptExecutor) driver).executeScript("// Remover modales y backdrops"
						+ "document.querySelectorAll('.modal-backdrop, .modal, .overlay, [class*=\"overlay\"]').forEach(el => el.remove());"
						+ "// Remover clase modal-open del body" + "document.body.classList.remove('modal-open');"
						+ "document.body.style.overflow = '';"
						+ "// Remover cualquier elemento con z-index alto que no sea el calendario"
						+ "document.querySelectorAll('*').forEach(el => {"
						+ "    const zIndex = window.getComputedStyle(el).zIndex;"
						+ "    if (zIndex > 1000 && !el.id.includes('calendar') && !el.classList.contains('calendar')) {"
						+ "        el.style.zIndex = '1';" + "    }" + "});");
				System.out.println("🧹 Overlays limpiados");
			} catch (Exception e) {
				// Ignorar si falla
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
			case "lunes":
				return 0;
			case "martes":
				return 1;
			case "miércoles":
			case "miercoles":
				return 2;
			case "jueves":
				return 3;
			case "viernes":
				return 4;
			case "sábado":
			case "sabado":
				return 5;
			case "domingo":
				return 6;
			default:
				return -1;
			}
		}
	}
