package graph.main;

import com.mxgraph.swing.mxGraphComponent;
import exceptions.CycleFoundException;
import exceptions.VertexDuplicationException;
import exceptions.VertexIdUndefinedException;
import graph.*;
import graph.model.CourseEdge;
import graph.model.CourseVertex;
import graph.model.MxGraphWrapper;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.List;

import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

/**
 * Created by Yury on 05.12.2016.
 */
public class MainWindow extends JFrame {

    private final FileDialog fileDialog;
    private final MxGraphWrapper mxGraphWrapper;
    private long lastActionMillis = -1;

    public MainWindow() throws IOException {
        GraphProvider graphProvider = new FileGraphProviderImpl("courseGraph");
        DirectedAcyclicGraph<CourseVertex, CourseEdge> rawGraph = null;
        try {
            rawGraph = graphProvider.getGraph();
        } catch (IOException e) {
            Utils.showErrorMessageAndExit("Файл с графом курсов не найден");
        } catch (VertexDuplicationException e) {
            Utils.showErrorMessageAndExit("Дупликация вершины с id = " + e.getDuplicatedId());
        } catch (VertexIdUndefinedException e) {
            Utils.showErrorMessageAndExit("Не найдено вершины с id = " + e.getId());
        } catch (CycleFoundException e) {
            Utils.showErrorMessageAndExit("Ребро \"" + e.getFrom() + "\"->\"" + e.getTo() + "\" создает цикл");
        }
        fileDialog = new FileDialog(this, "Выберите файл", FileDialog.SAVE);

        mxGraphWrapper = new MxGraphWrapper(rawGraph);
        mxGraphComponent graphComponent = mxGraphWrapper.getGraphComponent();
        graphComponent.setEnabled(true);
        graphComponent.setConnectable(false);
        graphComponent.setWheelScrollingEnabled(true);
        graphComponent.getGraphHandler().setMarkerEnabled(true);
        graphComponent.getGraphControl().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                onMove(e, graphComponent);
            }
        });
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    onLeftClick(e, graphComponent);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                onMove(e, graphComponent);
            }
        });
        graphComponent.setSize(new Dimension(750, 750));
        graphComponent.setBorder(null);
        ((JComponent)getComponent(0)).getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK),
                "revertLastAction");
        ((JComponent)getComponent(0)).getActionMap().put("revertLastAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mxGraphWrapper.revertPreviousAction();
            }
        });
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(graphComponent, BorderLayout.CENTER);
        JButton exportResultButton = new JButton("Сохранить выбор...");
        exportResultButton.setVisible(true);
        exportResultButton.addActionListener(event -> {
            List<String> selectedCourses = mxGraphWrapper.getSelectedCourses();
            fileDialog.setVisible(true);
            try {
                String filename = fileDialog.getFile();
                Files.write(Paths.get(filename), selectedCourses, StandardOpenOption.CREATE);
                Utils.showInfoMessage("Выбранные курсы успешно записаны в файл " + filename);
            } catch (IOException e) {
                Utils.showErrorMessage("Ошибка записи");
            }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout());
        panel.add(exportResultButton);
        getContentPane().setLayout(new FlowLayout(FlowLayout.LEADING));
        getContentPane().add(panel);
    }

    private void onLeftClick(MouseEvent e, mxGraphComponent graphComponent) {
        Optional<CourseVertex> target = Utils.getCourseVertexFromEvent(e, graphComponent);
        target.ifPresent(clickedVertex -> {
            if (!clickedVertex.isChoosen()) {
                mxGraphWrapper.selectAllParents(clickedVertex);
            } else {
                mxGraphWrapper.disselectAllChildren(clickedVertex);
            }
        });
    }

    private void onMove(MouseEvent e, mxGraphComponent graphComponent) {
        Optional<CourseVertex> target = Utils.getCourseVertexFromEvent(e, graphComponent);
        if (target.isPresent() && !mxGraphWrapper.hasOpaqueVertices()) {
            long currentTime = System.currentTimeMillis();
            if (lastActionMillis + 100 < currentTime) {
                lastActionMillis = currentTime;
                mxGraphWrapper.highlightAllChildren(target.get());
            }
        }
        if (!target.isPresent() && mxGraphWrapper.hasOpaqueVertices()) {
            mxGraphWrapper.removeOpaqueVertices();
        }
    }


}
