package com.saul.loginGym.dto;

public class ClaseObjetivo {
	public final String dia;         // "LUNES", "MARTES", ...
	public final String nombre;      // "POWER VIRTUAL"
	public final String rangoHora;   // "07:00 / 08:00"
    public ClaseObjetivo(String dia, String nombre, String rangoHora) {
        this.dia = dia; this.nombre = nombre; this.rangoHora = rangoHora;
    }
}
