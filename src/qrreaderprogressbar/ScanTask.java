/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qrreaderprogressbar;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import qrreaderprogressbar.scan.QRCodeReader;

/**
 *
 * @author Artem.Kovalev
 */
public class ScanTask extends Task<ObservableList<File>> {
    
    public ReadOnlyObjectWrapper<String> successratiostr = new ReadOnlyObjectWrapper<>(new String());
    public ReadOnlyObjectWrapper<String> progresstr = new ReadOnlyObjectWrapper<>(new String());
    private File dir;
    private Map<Path,String> map;
    
    public ScanTask(File dir) {
        this.dir = dir;
        map = new HashMap<>();
    }
    
    public Map<Path,String> getMap() {
        return map;
    }
    
        
    @Override
    protected ObservableList<File> call() throws Exception {
        
        FilenameFilter filter = new FilenameFilter() { 
            public boolean accept(File f, String name){ 
                return name.toLowerCase().endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".bmp") || name.endsWith(".png") || name.endsWith(".tiff") || name.endsWith(".webp"); 
            } 
        }; 
        FilenameFilter antifilter = new FilenameFilter() { 
            public boolean accept(File f, String name){ 
                return !(name.toLowerCase().endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".bmp") || name.endsWith(".png") || name.endsWith(".tiff") || name.endsWith(".webp")); 
            } 
        };
        
        QRCodeReader.readProperties();
        
        ObservableList<File> scanned = FXCollections.observableArrayList();//
        
        int read = 0;//счетчик успешных срабатываний
        
        int i=0;//счетчик обработанных файлов
        
        int count=dir.listFiles(filter).length;//количество файлов в папке
        
        for (File f:dir.listFiles(filter))
        {
            if (this.scan(f))
                read++;
            scanned.add(f);
            i++;
            this.updateMessage(f.getName());
            this.updateProgress(i, count);
            String ss = String.format("Распознаваемость: %d/%d = %.2f",read,i,(read+0.0)/i);
            String ps = i+"/"+count;
            Platform.runLater(() -> {
                successratiostr.setValue(ss); 
                progresstr.setValue(ps);
            });
        }
        for (File f:dir.listFiles(antifilter))
        {
            map.put(f.toPath(), "Неподдерживаемый формат файла");
        }
        return scanned;
    }
    
    

    private boolean scan(File f) {
        String res = QRCodeReader.scan(f);
        map.put(f.toPath(), res);
        return (res.startsWith("t="));
    }
}
    

