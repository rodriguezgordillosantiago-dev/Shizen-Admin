package com.shizen.Admin.dto;

/** Categoría y cantidad de unidades pedidas dentro del período consultado. */
public record CategoriaEstadistica(
        String categoria,
        long cantidad
) {
}
