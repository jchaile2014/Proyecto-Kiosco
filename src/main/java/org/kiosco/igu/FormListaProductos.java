package org.kiosco.igu;

import org.kiosco.logica.Producto;
import org.kiosco.persistencia.ProductoDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class FormListaProductos extends JFrame {

    public FormListaProductos() {
        setTitle("Listado de Productos");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        String[] columnas = {"Código", "Nombre", "Categoría", "Precio", "Stock"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0);

        try {
            ProductoDAO dao = new ProductoDAO();
            List<Producto> productos = dao.listarTodos();

            for (Producto p : productos) {
                Object[] fila = {p.getCodigo(), p.getNombre(), p.getCategoria(), p.getPrecio(), p.getStock()};
                modelo.addRow(fila);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el listado: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        JTable tabla = new JTable(modelo);
        JScrollPane scroll = new JScrollPane(tabla);

        add(scroll, BorderLayout.CENTER);
    }
}
