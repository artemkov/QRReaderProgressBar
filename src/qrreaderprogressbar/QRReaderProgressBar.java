/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qrreaderprogressbar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import qrreaderprogressbar.scan.QRCodeReader;

/**
 *
 * @author Artem.Kovalev
 */
public class QRReaderProgressBar extends Application {
    private File dir;
    
    
    @Override
    public void start(Stage primaryStage) {
        
        System.out.println(System.getProperty("java.vm.name"));
        
        ProgressBar progressBar = new ProgressBar(0);
        Button selectdirtbtn = new Button();
        Button startbtn = new Button();
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        Label dirlabel = new Label();
        Label cntlabel = new Label();
        Label statusLabel= new Label();
        Label resLabel= new Label();
        Label timeLabel= new Label();
        Label paramLabel= new Label();
        
        QRCodeReader.readProperties();
        
        paramLabel.setText("J64:"+QRCodeReader.isB64()+"\tStep:"+QRCodeReader.getcStep()+"\tMS:"+QRCodeReader.getcMinsize()+"\tRQ:"+QRCodeReader.getMethod().name());
        
        
        
        selectdirtbtn.setText("Select path");
        startbtn.setText("Start");
        startbtn.setVisible(false);
        progressBar.setVisible(false);
        
        
        FilenameFilter filter = new FilenameFilter() { 
                    public boolean accept(File f, String name) 
                        { 
                            return name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".bmp") || name.endsWith(".png") || name.endsWith(".webp"); 
                        } 
                }; 
        
        selectdirtbtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                dir =  directoryChooser.showDialog(primaryStage);
                if (dir!=null)
                {
                    cntlabel.textProperty().unbind();
                    cntlabel.setText("файлов: "+dir.listFiles(filter).length);
                    dirlabel.setText(dir.getAbsolutePath());
                    startbtn.setVisible(true);
                    startbtn.setDisable(false);
                    progressBar.setVisible(true);
                    progressBar.setProgress(0);
                }
            }
        });
        
        
        
        
        
        startbtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (dir==null)
                    return;
                java.util.Timer timer = new java.util.Timer();
                TimerTask timerTask = new TimerTask() {
                    long count=0;
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            count++;
                            int[] comps = splitToComponentTimes(count);
                            timeLabel.setText(String.format("%02d:%02d:%02d",comps[0],comps[1],comps[2]));
                        });
                    }
                };
                
                
                
                startbtn.setDisable(true);
                selectdirtbtn.setDisable(true);
                // Create a Task.
                ScanTask scanTask = new ScanTask(dir);
                // Unbind progress property
                progressBar.progressProperty().unbind();
                // Bind progress property
                progressBar.progressProperty().bind(scanTask.progressProperty());
                // Unbind text property for Label.
                statusLabel.textProperty().unbind();
                // Bind the text property of Label
                // with message property of Task
                statusLabel.textProperty().bind(scanTask.messageProperty());
                
                cntlabel.textProperty().unbind();
                cntlabel.textProperty().bind(scanTask.progresstr);
                
                resLabel.textProperty().unbind();
                resLabel.textProperty().bind(scanTask.successratiostr);
                
                
                timer.schedule(timerTask, 0, 1000);
                
                scanTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, //
                       new EventHandler<WorkerStateEvent>() {
 
                           @Override
                           public void handle(WorkerStateEvent t) {
                                timer.cancel();
                                startbtn.setDisable(false);
                                selectdirtbtn.setDisable(false);
                                progressBar.progressProperty().unbind();
                                //List<File> scanned = scanTask.getValue();
                                statusLabel.textProperty().unbind();
                                statusLabel.setText("Запись в файл...");
                                Map<Path,String> map = scanTask.getMap();
                                BufferedWriter writer=null;
                                
                                try {
                                    /*File f= new File("out.txt");
                                    if (f.exists())
                                        f.delete();*/
                                    writer = new BufferedWriter(new FileWriter(new SimpleDateFormat("yyyyMMddHHmm").format(new Date())+"_out.txt"));
                                    int n=0;
                                    int tot = map.size();
                                    for (Entry <Path,String> me : map.entrySet())
                                    {
                                        if (me.getValue().startsWith("t="))
                                            n++;
                                        writer.append(me.getKey().getFileName()+"\t"+me.getValue());
                                        writer.newLine();
                                    }
                                    double ratio = (double)n/tot;
                                    resLabel.textProperty().unbind();
                                    resLabel.setText(String.format("Распознаваемость: %d/%d = %.2f",n,tot,ratio));
                                } catch (IOException ex) {
                                   Logger.getLogger(QRReaderProgressBar.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                finally {
                                    try {
                                       statusLabel.setText("Сканирование завершено!!!");
                                       if (writer!=null)
                                        writer.close();
                                    } catch (IOException ex) {
                                        Logger.getLogger(QRReaderProgressBar.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }
                       });
                scanTask.addEventFilter(WorkerStateEvent.WORKER_STATE_CANCELLED,
                        new EventHandler<WorkerStateEvent>() {
                            @Override
                            public void handle(WorkerStateEvent event) {
                                System.out.println("Cancelled");
                            }
                            
                        }
                );
                scanTask.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED,
                        new EventHandler<WorkerStateEvent>() {
                            @Override
                            public void handle(WorkerStateEvent event) {
                                System.out.println("Failed");
                            }
                            
                        }
                );
                
                
 
                // Start the Task.
                new Thread(scanTask).start();
                //Platform.runLater(new Thread(timerTask));
            }
        });
        
        
        //panes
        BorderPane root = new BorderPane();
        FlowPane center = new FlowPane();
        FlowPane bottom = new FlowPane();
        
        //center
        center.setHgap(10);
        center.setVgap(10);
        selectdirtbtn.setPrefSize(100, 20);
        dirlabel.setPrefSize(300, 20);
        startbtn.setPrefSize(150, 20);
        statusLabel.setPrefSize(300, 20);
        resLabel.setPrefSize(250, 20);
        center.getChildren().add(paramLabel);
        center.getChildren().add(selectdirtbtn);
        center.getChildren().add(dirlabel);
        center.getChildren().add(startbtn);
        center.getChildren().add(statusLabel);
        center.getChildren().add(resLabel);
        center.getChildren().add(timeLabel);
        
        //bottom
        bottom.setHgap(10);
        progressBar.setPrefSize(180, 20);
        cntlabel.setPrefSize(100, 20);
        bottom.getChildren().add(progressBar);
        bottom.getChildren().add(cntlabel);
        
        
        
        
        //root
        root.setPadding(new Insets(15,15,15,15));
        root.setCenter(center);
        root.setBottom(bottom);
        
        Scene scene = new Scene(root, 320, 260);
        
        primaryStage.setTitle("QR Code Reader");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                
                QRCodeReader.closeLog();
                Platform.exit();
                System.exit(0);
            }
        });
    }

    private static int[] splitToComponentTimes(long seconds)
    {
        
        int hours = (int) seconds / 3600;
        int remainder = (int) seconds - hours * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;

        int[] ints = {hours , mins , secs};
        return ints;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
