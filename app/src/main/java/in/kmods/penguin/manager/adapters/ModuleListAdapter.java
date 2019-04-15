package in.kmods.penguin.manager.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import in.kmods.penguin.manager.PenguinApp;
import in.kmods.penguin.manager.R;
import in.kmods.penguin.manager.models.ModuleData;

public class ModuleListAdapter extends RecyclerView.Adapter {
    private static ModuleListAdapter _instance;

    public static ModuleListAdapter getInstance(List<ModuleData> moduleDatalist, boolean force){
        if(_instance == null || force){
            _instance = new ModuleListAdapter(moduleDatalist);
        }
        return _instance;
    }

    private List<ModuleData> moduleDatalist;

    private ModuleListAdapter(List<ModuleData> moduleDatalist){
        this.moduleDatalist = moduleDatalist;
    }

    public void add(ModuleData data){
        moduleDatalist.add(data);
        notifyDataSetChanged();
    }

    public void remove(String pkg){
        for(ModuleData moduleData : moduleDatalist){
            if(pkg.equals(moduleData.packageName)){
                moduleDatalist.remove(moduleData);
                notifyDataSetChanged();
                break;
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_card_small, parent, false);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        View view = holder.itemView.findViewById(R.id.card_view);
        ModuleData moduleData = moduleDatalist.get(position);

        ImageView icon = view.findViewById(R.id.module_icon);
        icon.setImageDrawable(moduleData.icon);

        TextView appName = view.findViewById(R.id.module_name);
        appName.setText(moduleData.appName + " " + moduleData.versionName);

        TextView desc = view.findViewById(R.id.module_desc);
        desc.setText(moduleData.description.trim());

        AppCompatCheckBox checkBox = view.findViewById(R.id.module_control);
        checkBox.setChecked(moduleData.isEnabled || isPackageEnable(moduleData.packageName));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            moduleData.isEnabled = isChecked;
            UpdateModuleList();
        });

        setAnimation(holder.itemView);
    }

    @Override
    public int getItemCount() {
        return moduleDatalist.size();
    }

    public void UpdateModuleList(){
        File modulelist = new File(PenguinApp.getApp().getDir("config", 0), "modules.list");
        try {
            FileWriter fw = new FileWriter(modulelist);
            for(ModuleData data : moduleDatalist){
                if(data.isEnabled) {
                    fw.write(data.sourceDir + "\n");
                }
            }
            fw.close();
            modulelist.setReadable(true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isPackageEnable(String pkg){
        File modulelist = new File(PenguinApp.getApp().getDir("config", 0), "modules.list");
        try {
            FileReader reader = new FileReader(modulelist);
            char[] buff = new char[(int) modulelist.length()];
            reader.read(buff, 0 , buff.length);
            reader.close();
            String data = new String(buff);
            if(data.contains(pkg)){
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void setAnimation(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(1000);
        view.startAnimation(anim);
    }
}
