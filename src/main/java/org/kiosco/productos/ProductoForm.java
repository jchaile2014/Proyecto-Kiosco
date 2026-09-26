package org.kiosco.productos;

/** Formulario de producto. Los números llegan como texto para poder explicar si están mal escritos. */
public class ProductoForm {

    private String codigo;
    private String nombre;
    private String precio;
    private boolean controlaStock;
    private String stock;
    private String stockMinimo;

    static ProductoForm de(Producto producto, String precioEscrito) {
        ProductoForm form = new ProductoForm();
        form.codigo = producto.getCodigo();
        form.nombre = producto.getNombre();
        form.precio = precioEscrito;
        form.controlaStock = producto.isControlaStock();
        form.stock = producto.isControlaStock() ? String.valueOf(producto.getStock()) : "";
        form.stockMinimo = producto.getStockMinimo() == null ? "" : String.valueOf(producto.getStockMinimo());
        return form;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPrecio() {
        return precio;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }

    public boolean isControlaStock() {
        return controlaStock;
    }

    public void setControlaStock(boolean controlaStock) {
        this.controlaStock = controlaStock;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(String stockMinimo) {
        this.stockMinimo = stockMinimo;
    }
}
