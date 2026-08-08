package org.kiosco.igu;

import org.kiosco.logica.Compra;
import org.kiosco.logica.DetalleCompra;
import org.kiosco.logica.Producto;
import org.kiosco.persistencia.CompraDAO;
import org.kiosco.persistencia.DetalleCompraDAO;
import org.kiosco.persistencia.ProductoDAO;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class FormCompra extends JFrame {

    private JTextField txtProveedor;
    private JTextField txtCodigoProducto;
    private JTextField txtCantidad;
    private JTextField txtPrecioCosto;

    public FormCompra() {
        setTitle("Registrar Compra");
        setSize(350, 280);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));

        panel.add(new JLabel("Proveedor:"));
        txtProveedor = new JTextField();
        panel.add(txtProveedor);

        panel.add(new JLabel("Código producto:"));
        txtCodigoProducto = new JTextField();
        panel.add(txtCodigoProducto);

        panel.add(new JLabel("Cantidad:"));
        txtCantidad = new JTextField();
        panel.add(txtCantidad);

        panel.add(new JLabel("Precio costo unitario:"));
        txtPrecioCosto = new JTextField();
        panel.add(txtPrecioCosto);

        JButton btnRegistrar = new JButton("Registrar compra");
        panel.add(btnRegistrar);

        add(panel);

        btnRegistrar.addActionListener(e -> {
            try {
                registrarCompra();
            } catch (Exception ex) {
                FormUtils.manejarError(this, ex);
            }
        });
    }

    private void registrarCompra() {
        String proveedor = FormUtils.requireTexto(txtProveedor, "Proveedor");
        int codigoProducto = FormUtils.parseEntero(txtCodigoProducto, "Código producto");
        int cantidad = FormUtils.parseEnteroPositivo(txtCantidad, "Cantidad");
        double precioCosto = FormUtils.parsePositivo(txtPrecioCosto, "Precio costo unitario");

        ProductoDAO productoDAO = new ProductoDAO();
        Producto producto = productoDAO.buscarPorCodigo(codigoProducto);

        if (producto == null) {
            JOptionPane.showMessageDialog(this, "Producto no encontrado.");
            return;
        }

        double total = cantidad * precioCosto;

        Compra compra = new Compra(proveedor, LocalDate.now(), total);
        CompraDAO compraDAO = new CompraDAO();
        compraDAO.guardar(compra);

        DetalleCompra detalle = new DetalleCompra(compra, producto, cantidad, precioCosto);
        DetalleCompraDAO detalleDAO = new DetalleCompraDAO();
        detalleDAO.guardar(detalle);

        productoDAO.actualizarStock(codigoProducto, cantidad);

        JOptionPane.showMessageDialog(this, "Compra registrada. Total: $" + total);
        dispose();
    }
}
