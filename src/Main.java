import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.stream.Collectors;

public class Main {

    // La ruta de nuestra base de datos oficial (Se creará como un archivo en tu proyecto)
    private static final String DB_URL = "jdbc:sqlite:inventario.db";

    public static void main(String[] args) {
        try {
            // 🌟 ESTA ES LA LÍNEA MÁGICA QUE OBLIGA A JAVA A LEER EL DRIVER:
            Class.forName("org.sqlite.JDBC");

            // 1. Inicializar la Base de Datos (Crea el archivo y la tabla si no existen)
            inicializarBaseDeDatos();

            // 2. Encender el Servidor Web
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/api/inventory", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    // Habilitamos los permisos CORS para Angular
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

                    String metodo = exchange.getRequestMethod();
                    String path = exchange.getRequestURI().getPath();

                    if ("OPTIONS".equalsIgnoreCase(metodo)) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }

                    // Abrimos la conexión oficial con SQLite
                    try (Connection conn = DriverManager.getConnection(DB_URL)) {

                        // --- 1. OBTENER TODO (GET) ---
                        if ("GET".equalsIgnoreCase(metodo) && path.equals("/api/inventory")) {
                            String sql = "SELECT * FROM items";
                            Statement stmt = conn.createStatement();
                            ResultSet rs = stmt.executeQuery(sql);

                            StringBuilder json = new StringBuilder("[");
                            boolean hasNext = rs.next();
                            while (hasNext) {
                                json.append(String.format(
                                        "{\"id\":%d,\"name\":\"%s\",\"category\":\"%s\",\"quantity\":%d}",
                                        rs.getInt("id"), rs.getString("name"), rs.getString("category"), rs.getInt("quantity")
                                ));
                                hasNext = rs.next();
                                if (hasNext) json.append(",");
                            }
                            json.append("]");
                            responderJson(exchange, json.toString(), 200);
                            return;
                        }

                        // --- 2. AGREGAR NUEVO (POST) ---
                        if ("POST".equalsIgnoreCase(metodo) && path.equals("/api/inventory")) {
                            String body = leerCuerpo(exchange);
                            String name = extraerCampoJson(body, "name");
                            String category = extraerCampoJson(body, "category");
                            int quantity = parsearEntero(extraerCampoJson(body, "quantity"));

                            String sql = "INSERT INTO items(name, category, quantity) VALUES(?,?,?)";
                            PreparedStatement pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, name);
                            pstmt.setString(2, category);
                            pstmt.setInt(3, quantity);
                            pstmt.executeUpdate();

                            System.out.println("➕ Guardado en Base de Datos: " + name);
                            responderJson(exchange, "{\"message\":\"Agregado con éxito\"}", 201);
                            return;
                        }

                        // --- 3. EDITAR MATERIAL (PUT) ---
                        if ("PUT".equalsIgnoreCase(metodo) && path.startsWith("/api/inventory/")) {
                            Long idAEditar = Long.parseLong(path.substring("/api/inventory/".length()));
                            String body = leerCuerpo(exchange);
                            String newName = extraerCampoJson(body, "name");
                            String newCategory = extraerCampoJson(body, "category");
                            int newQuantity = parsearEntero(extraerCampoJson(body, "quantity"));

                            String sql = "UPDATE items SET name = ?, category = ?, quantity = ? WHERE id = ?";
                            PreparedStatement pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, newName);
                            pstmt.setString(2, newCategory);
                            pstmt.setInt(3, newQuantity);
                            pstmt.setLong(4, idAEditar);
                            int filasAfectadas = pstmt.executeUpdate();

                            if (filasAfectadas > 0) {
                                System.out.println("✏️ Editado en Base de Datos. ID: " + idAEditar);
                                responderJson(exchange, "{\"message\":\"Editado con éxito\"}", 200);
                            } else {
                                responderJson(exchange, "{\"message\":\"No encontrado\"}", 404);
                            }
                            return;
                        }

                        // --- 4. BORRAR MATERIAL (DELETE) ---
                        if ("DELETE".equalsIgnoreCase(metodo) && path.startsWith("/api/inventory/")) {
                            Long idAEliminar = Long.parseLong(path.substring("/api/inventory/".length()));

                            String sql = "DELETE FROM items WHERE id = ?";
                            PreparedStatement pstmt = conn.prepareStatement(sql);
                            pstmt.setLong(1, idAEliminar);
                            int filasAfectadas = pstmt.executeUpdate();

                            if (filasAfectadas > 0) {
                                System.out.println("🗑️ Borrado de Base de Datos. ID: " + idAEliminar);
                                responderJson(exchange, "{\"message\":\"Borrado con éxito\"}", 200);
                            } else {
                                responderJson(exchange, "{\"message\":\"No encontrado\"}", 404);
                            }
                            return;
                        }

                        responderJson(exchange, "{\"message\":\"Ruta no encontrada\"}", 404);

                    } catch (SQLException e) {
                        e.printStackTrace();
                        responderJson(exchange, "{\"error\":\"Error de la Base de Datos\"}", 500);
                    }
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("🚀 ¡SERVIDOR CORRIENDO CON BASE DE DATOS SQLITE EN EL PUERTO 8080!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    //   FUNCIONES AUXILIARES DE APOYO
    // ==========================================

    private static void inicializarBaseDeDatos() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                // Creamos la tabla de SQL
                String sql = "CREATE TABLE IF NOT EXISTS items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "category TEXT," +
                        "quantity INTEGER" +
                        ");";
                Statement stmt = conn.createStatement();
                stmt.execute(sql);
                System.out.println("✅ Base de datos SQLite creada y enlazada con éxito.");

                // Si la tabla está totalmente vacía, le insertamos los materiales de prueba iniciales
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM items");
                if(rs.next() && rs.getInt("total") == 0) {
                    stmt.execute("INSERT INTO items(name, category, quantity) VALUES ('Lámpara Acrílica CreaNova', 'Diseño', 15)");
                    stmt.execute("INSERT INTO items(name, category, quantity) VALUES ('Plancha MDF 3mm', 'Materia Prima', 50)");
                    stmt.execute("INSERT INTO items(name, category, quantity) VALUES ('Vinilo UV Premium', 'Sublimación', 20)");
                    System.out.println("📦 Productos iniciales cargados en la base de datos.");
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error creando la tabla: " + e.getMessage());
        }
    }

    private static String leerCuerpo(HttpExchange exchange) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        return reader.lines().collect(Collectors.joining());
    }

    private static String extraerCampoJson(String json, String campo) {
        String clave = "\"" + campo + "\":";
        int indexClave = json.indexOf(clave);
        if (indexClave == -1) return "";
        int inicioValor = indexClave + clave.length();
        char primerChar = json.charAt(inicioValor);
        if (primerChar == '"') {
            inicioValor++;
            int finValor = json.indexOf("\"", inicioValor);
            return json.substring(inicioValor, finValor);
        } else {
            int finValor = json.indexOf(",", inicioValor);
            if (finValor == -1) finValor = json.indexOf("}", inicioValor);
            return json.substring(inicioValor, finValor).trim();
        }
    }

    private static int parsearEntero(String valor) {
        try { return Integer.parseInt(valor); } catch (Exception e) { return 0; }
    }

    private static void responderJson(HttpExchange exchange, String jsonResponse, int codigoRespuesta) throws IOException {
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(codigoRespuesta, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}