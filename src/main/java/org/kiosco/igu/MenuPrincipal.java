package org.kiosco.igu;

import javax.swing.*;
import java.awt.*;

public class MenuPrincipal extends JFrame {

    public MenuPrincipal() {
        setTitle("Kiosco - Menú Principal");
        setSize(400, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(9, 1, 10, 10));

        JButton btnAgregar = new JButton("Agregar producto");
        JButton btnListar = new JButton("Listar productos");
        JButton btnBuscar = new JButton("Buscar producto");
        JButton btnEliminar = new JButton("Dar de baja producto");
        JButton btnCliente = new JButton("Agregar cliente");
        JButton btnRepartidor = new JButton("Agregar repartidor");
        JButton btnCompra = new JButton("Registrar compra");
        JButton btnVenta = new JButton("Registrar venta");
        JButton btnFiado = new JButton("Registrar fiado");

        panel.add(btnAgregar);
        panel.add(btnListar);
        panel.add(btnBuscar);
        panel.add(btnEliminar);
        panel.add(btnCliente);
        panel.add(btnRepartidor);
        panel.add(btnCompra);
        panel.add(btnVenta);
        panel.add(btnFiado);

        add(panel);

        btnAgregar.addActionListener(e -> {
            FormProducto form = new FormProducto();
            form.setVisible(true);
        });

        btnListar.addActionListener(e -> {
            FormListaProductos form = new FormListaProductos();
            form.setVisible(true);
        });

        btnBuscar.addActionListener(e -> {
            FormBuscarProducto form = new FormBuscarProducto();
            form.setVisible(true);
        });

        btnEliminar.addActionListener(e -> {
            FormEliminarProducto form = new FormEliminarProducto();
            form.setVisible(true);
        });

        btnCliente.addActionListener(e -> {
            FormCliente form = new FormCliente();
            form.setVisible(true);
        });

        btnRepartidor.addActionListener(e -> {
            FormRepartidor form = new FormRepartidor();
            form.setVisible(true);
        });

        btnCompra.addActionListener(e -> {
            FormCompra form = new FormCompra();
            form.setVisible(true);
        });

        btnVenta.addActionListener(e -> {
            FormVenta form = new FormVenta();
            form.setVisible(true);
        });

        btnFiado.addActionListener(e -> {
            FormFiado form = new FormFiado();
            form.setVisible(true);
        });
    }
}