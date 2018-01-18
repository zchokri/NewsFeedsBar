package czdev.newsfeedsbar;

/**
 * Created by ZAGROUBA CHOKRI on 02/01/2018.
 */

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class CustomListAdapter  extends BaseAdapter {

    private List<FeedMessage> listData;
    private LayoutInflater layoutInflater;
    private Context context;

    public CustomListAdapter(Context aContext,  List<FeedMessage> listData) {
        this.context = aContext;
        this.listData = listData;
        layoutInflater = LayoutInflater.from(aContext);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_item_layout, null);
            holder = new ViewHolder();
            holder.flagView = (ImageView) convertView.findViewById(R.id.imageItem);
            holder.titleView = (TextView) convertView.findViewById(R.id.textView_title);
            holder.descView = (TextView) convertView.findViewById(R.id.textView_desc);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FeedMessage feedMessage = this.listData.get(position);
        holder.titleView.setText(feedMessage.getTitle());
        holder.titleView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        holder.descView.setText(feedMessage.getDescription());
       // int imageId = this.getMipmapResIdByName();
        if(feedMessage.getLink().contains("cnn")) {
            holder.flagView.setImageResource(R.drawable.cnn2);
        }else if(feedMessage.getLink().contains("cnn"))
            {
            holder.flagView.setImageResource(R.drawable.f24);

        } if(feedMessage.getLink().contains("jaze")) {
            holder.flagView.setImageResource(R.drawable.jsc);

        }

            return convertView;
    }

    // Find Image ID corresponding to the name of the image (in the directory mipmap).
    public int getMipmapResIdByName(String resName)  {
        String pkgName = context.getPackageName();
        // Return 0 if not found.
        int resID = context.getResources().getIdentifier(resName , "mipmap", pkgName);
        Log.i("CustomListView", "Res Name: "+ resName+"==> Res ID = "+ resID);
        return resID;
    }

    static class ViewHolder {
        ImageView flagView;
        TextView titleView;
        TextView descView;
    }

}