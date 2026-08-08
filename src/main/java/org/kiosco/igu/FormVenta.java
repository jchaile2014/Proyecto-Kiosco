package org.kiosco.igu;

import org.kiosco.logica.Venta;
import org.kiosco.logica.DetalleVenta;
import org.kiosco.logica.Producto;
import org.kiosco.persistencia.VentaDAO;
import org.kiosco.persistencia.DetalleVentaDAO;
import org.kiosco.persistencia.ProductoDAO;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class FormVenta extends JFrame {

    private JTextField txtCodigoProducto;
    private JTextField txtCantidad;

    public FormVenta() {
        setTitle("Registrar Venta");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));

        panel.add(new JLabel("Código producto:"));
        txtCodigoProducto = new JTextField();
        panel.add(txtCodigoProducto);

        panel.add(new JLabel("Cantidad:"));
        txtCantidad = new JTextField();
        panel.add(txtCantidad);

        JButton btnRegistrar = new JButton("Registrar venta");
        panel.add(btnRegistrar);

        add(panel);

        btnRegistrar.addActionListener(e -> {
            try {
                registrarVenta();
            } catch (Exception ex) {
                FormUtils.manejarError(this, ex);
            }
        });
    }

    private void registrarVenta() {
        int codigoProducto = FormUtils.parseEntero(txtCodigoProducto, "Código producto");
        int cantidad = FormUtils.parseEnteroPositivo(txtCantidad, "Cantidad");

        ProductoDAO productoDAO = new ProductoDAO();
        Producto producto = productoDAO.buscarPorCodigo(codigoProducto);

        if (producto == null) {
            JOptionPane.showMessageDialog(this, "Producto no encontrado.");
            return;
        }

        if (producto.getStock() < cantidad) {
            JOptionPane.showMessageDialog(this, "Stock insuficiente. Disponible: " + producto.getStock());
            return;
        }

        double total = cantidad * producto.getPrecio();

        Venta venta = new Venta(LocalDate.now(), total);
        VentaDAO ventaDAO = new VentaDAO();
        ventaDAO.guardar(venta);

        DetalleVenta detalle = new DetalleVenta(venta, producto, cantidad, producto.getPrecio());
        DetalleVentaDAO detalleDAO = new DetalleVentaDAO();
        detalleDAO.guardar(detalle);

        productoDAO.actualizarStock(codigoProducto, -cantidad);

        JOptionPane.showMessageDialog(this, "Venta registrada. Total: $" + total);
        dispose();
    }
}
