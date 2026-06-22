package model;

public class Usuario {
    // Attributes: Las propiedades reales del objeto
    private Long id;
    private String nombre;
    private String correo;
    private String password; // El dato sensible que no debe salir de aquí

    // Constructor: La función que nos permite fabricar un usuario nuevo
    public Usuario(Long id, String nombre, String correo, String password) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.password = password;
    }

    // Getters: Los métodos públicos para poder leer los datos desde afuera
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPassword() { return password; }
}