package com.reservas.exception;

/**
 * Excepción lanzada cuando se excede un límite del plan
 */
public class LimiteExcedidoException extends RuntimeException {

    private final String recurso;
    private final int limiteActual;
    private final int limiteMaximo;

    public LimiteExcedidoException(String recurso, int limiteActual, int limiteMaximo) {
        super(String.format("Límite excedido para %s. Actual: %d, Máximo permitido: %d",
                          recurso, limiteActual, limiteMaximo));
        this.recurso = recurso;
        this.limiteActual = limiteActual;
        this.limiteMaximo = limiteMaximo;
    }

    public LimiteExcedidoException(String mensaje) {
        super(mensaje);
        this.recurso = "";
        this.limiteActual = 0;
        this.limiteMaximo = 0;
    }

    public String getRecurso() {
        return recurso;
    }

    public int getLimiteActual() {
        return limiteActual;
    }

    public int getLimiteMaximo() {
        return limiteMaximo;
    }
}
