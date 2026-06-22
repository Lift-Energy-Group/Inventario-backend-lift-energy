package dto;

public class UsuarioDTO {

    private Long id;
    private String nombre;
    private String correo;

    // Constructor del DTO
    public UsuarioDTO(Long id, String nombre, String correo) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
    }


    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
}