package org.kiosco.igu;

import org.kiosco.logica.Producto;
import org.kiosco.persistencia.ProductoDAO;
import javax.swing.*;
import java.awt.*;

public class FormProducto extends JFrame {

    private JTextField txtNombre;
    private JTextField txtCategoria;
    private JTextField txtPrecio;
    private JTextField txtPrecioCosto;
    private JTextField txtStock;

    public FormProducto() {
        setTitle("Agregar Producto");
        setSize(350, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 2, 5, 5));

        panel.add(new JLabel("Nombre:"));
        txtNombre = new JTextField();
        panel.add(txtNombre);

        panel.add(new JLabel("Categoría:"));
        txtCategoria = new JTextField();
        panel.add(txtCategoria);

        panel.add(new JLabel("Precio venta:"));
        txtPrecio = new JTextField();
        panel.add(txtPrecio);

        panel.add(new JLabel("Precio costo:"));
        txtPrecioCosto = new JTextField();
        panel.add(txtPrecioCosto);

        panel.add(new JLabel("Stock inicial:"));
        txtStock = new JTextField();
        panel.add(txtStock);

        JButton btnGuardar = new JButton("Guardar");
        panel.add(btnGuardar);

        add(panel);

        btnGuardar.addActionListener(e -> {
            try {
                guardarProducto();
            } catch (Exception ex) {
                FormUtils.manejarError(this, ex);
            }
        });
    }

    private void guardarProducto() {
        String nombre = FormUtils.requireTexto(txtNombre, "Nombre");
        String categoria = FormUtils.requireTexto(txtCategoria, "Categoría");
        double precio = FormUtils.parsePositivo(txtPrecio, "Precio venta");
        double precioCosto = FormUtils.parsePositivo(txtPrecioCosto, "Precio costo");
        int stock = FormUtils.parseEntero(txtStock, "Stock inicial");

        Producto p = new Producto(nombre, categoria, precio, precioCosto, 0, stock);

        ProductoDAO dao = new ProductoDAO();
        dao.guardar(p);

        JOptionPane.showMessageDialog(this, "Producto guardado con código: " + p.getCodigo());
        dispose();
    }
}
