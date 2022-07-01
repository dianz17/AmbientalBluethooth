package com.rob.bluetoothmodule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

public class ListAdapter extends BaseAdapter {
    Context context;
    List<DeviceInfo> deviceInfoList;

    public ListAdapter(Context context, List<DeviceInfo> deviceInfoList) {
        this.context = context;
        this.deviceInfoList = deviceInfoList;
    }

    @Override
    public int getCount() {
        return deviceInfoList.size();
    }

    @Override
    public Object getItem(int i) {
        return deviceInfoList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view1 = view;
        LayoutInflater inflater = LayoutInflater.from(context);
        view1 = inflater.inflate(R.layout.bluetooth_list, null);
        TextView txtName = view1.findViewById(R.id.txtName);
        TextView txtAddress = view1.findViewById(R.id.txtAddress);

        txtName.setText(deviceInfoList.get(i).getName());
        txtAddress.setText(deviceInfoList.get(i).getAddress());

        return view1;
    }
}
