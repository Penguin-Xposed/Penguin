package in.kmods.penguin.manager.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import in.kmods.penguin.manager.PenguinApp;
import in.kmods.penguin.manager.R;
import in.kmods.penguin.manager.interfaces.PackageCallBack;
import in.kmods.penguin.manager.utils.ModuleUtils;

public class MainActivity extends AppCompatActivity implements PackageCallBack {
    View emptyView;
    RecyclerView moduleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        emptyView = findViewById(R.id.emptyView);
        moduleView = findViewById(R.id.moduleslist);
        ModuleUtils.updateInstalledModules(null);
        ModuleUtils.AddCallBack(this);
        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_softreboot:
                try {
                    Runtime.getRuntime().exec("sudo setprop ctl.restart surfaceflinger; setprop ctl.restart zygote");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateStatus(){
        try {
            File modulelist = new File(PenguinApp.getApp().getDir("config", 0), "modules.list");
            if(!modulelist.exists())
                modulelist.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        View status_container = findViewById(R.id.status_container);
        ImageView status_img = findViewById(R.id.status_icon);
        TextView status_tv = findViewById(R.id.status_text);

        if(PenguinApp.getStatus() == 1) {
            status_tv.setText(getString(R.string.framework_active, PenguinApp.getVersion()));
            status_tv.setTextColor(getResources().getColor(R.color.darker_green));
            status_container.setBackgroundColor(getResources().getColor(R.color.darker_green));
            status_img.setImageResource(R.drawable.ic_check_circle);
        } else if(PenguinApp.getStatus() == 0) {
            status_tv.setText(getString(R.string.framework_not_active));
            status_tv.setTextColor(getResources().getColor(R.color.amber_500));
            status_container.setBackgroundColor(getResources().getColor(R.color.amber_500));
            status_img.setImageResource(R.drawable.ic_warning);
        } else if(PenguinApp.getStatus() < 0) {
            status_tv.setText(R.string.framework_not_installed);
            status_tv.setTextColor(getResources().getColor(R.color.warning));
            status_container.setBackgroundColor(getResources().getColor(R.color.warning));
            status_img.setImageResource(R.drawable.ic_error);
        }

        if(PenguinApp.getStatus() == 1) {
            emptyView.setVisibility((ModuleUtils.getListSize() > 0 ? View.GONE : View.VISIBLE));
            moduleView.setVisibility((ModuleUtils.getListSize() > 0 ? View.VISIBLE : View.GONE));

            moduleView.setLayoutManager(new LinearLayoutManager(this));
            moduleView.setHasFixedSize(true);
            moduleView.setAdapter(ModuleUtils.getAdapter(true));
            moduleView.invalidate();
        } else {
            moduleView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onListUpdate() {
        if(moduleView != null) {
            if (PenguinApp.getStatus() == 1) {
                emptyView.setVisibility((ModuleUtils.getListSize() > 0 ? View.GONE : View.VISIBLE));
                moduleView.setVisibility((ModuleUtils.getListSize() > 0 ? View.VISIBLE : View.GONE));

                moduleView.invalidate();
            } else {
                moduleView.setVisibility(View.GONE);
            }
        }
    }
}
