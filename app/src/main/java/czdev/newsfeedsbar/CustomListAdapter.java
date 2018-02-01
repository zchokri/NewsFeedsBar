package czdev.newsfeedsbar;

/**
 * Created by ZAGROUBA CHOKRI on 02/01/2018.
 */

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static android.content.ContentValues.TAG;
import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static czdev.newsfeedsbar.Constants.TAG_LOG;

public class CustomListAdapter  extends RecyclerView.Adapter<CustomListAdapter.CustomViewHolder> {

    private List<FeedMessage> listData;
    private LayoutInflater layoutInflater;
    private Context mContext;

    public CustomListAdapter(Context aContext,  List<FeedMessage> listData) {
        this.mContext = aContext;
        this.listData = listData;
        layoutInflater = LayoutInflater.from(aContext);
    }


    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_layout, null);

        CustomViewHolder viewHolder = new CustomViewHolder(view);
        return viewHolder;

    }



    @Override
    public void onBindViewHolder(CustomViewHolder customViewHolder, int position) {
        FeedMessage feedMessage = listData.get(position);

        // Set selected state; use a state list drawable to style the view
        //Setting text view title
        customViewHolder.titleView.setText(feedMessage.getTitle());
        customViewHolder.descView.setText(feedMessage.getDescription());
        customViewHolder.pubDateView.setText(feedMessage.getData());

        if(feedMessage.getLink().contains("cnn")) {
            customViewHolder.imageView.setImageResource(R.drawable.cnn2);
        }else if(feedMessage.getLink().contains("24"))
        {
            customViewHolder.imageView.setImageResource(R.drawable.f24);

        } if(feedMessage.getLink().contains("jaze")) {
            customViewHolder.imageView.setImageResource(R.drawable.jsc);

        } if(feedMessage.getLink().contains("bbc")) {
            customViewHolder.imageView.setImageResource(R.drawable.bbc);

        }
        //Handle click event on both title and image click
        customViewHolder.imageView.setOnClickListener(clickListener);
        customViewHolder.titleView.setOnClickListener(clickListener);
        customViewHolder.descView.setOnClickListener(clickListener);
        customViewHolder.shareView.setOnClickListener(clickShareListener);
        customViewHolder.pubDateView.setOnClickListener(clickListener);
        customViewHolder.imageView.setTag(customViewHolder);
        customViewHolder.titleView.setTag(customViewHolder);
        customViewHolder.descView.setTag(customViewHolder);
        customViewHolder.shareView.setTag(customViewHolder);
        customViewHolder.pubDateView.setTag(customViewHolder);

        animate(customViewHolder);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            CustomViewHolder holder = (CustomViewHolder) view.getTag();
            int position = holder.getLayoutPosition();
            if (position == RecyclerView.NO_POSITION) return;
            // Updating old as well as new positions
            FeedMessage feedItem = listData.get(position);
            Intent ViewIntent = new Intent(mContext, ViewURL.class);
            ViewIntent.putExtra("link", feedItem.getLink());
            ViewIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(ViewIntent);
        }
    };

    View.OnClickListener clickShareListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            CustomViewHolder holder = (CustomViewHolder) view.getTag();
            int position = holder.getLayoutPosition();
            if (position == RecyclerView.NO_POSITION) return;
            // Updating old as well as new positions
            FeedMessage feedItem = listData.get(position);
            Intent myShareIntent = new Intent(Intent.ACTION_SEND);
            myShareIntent.setType("text/plain");
            myShareIntent.putExtra(Intent.EXTRA_TEXT, feedItem.getLink() +" shared from: "
                    + "https://play.google.com/store/apps/details?id=czdev.newsfeedsbar");
            myShareIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(Intent.createChooser(myShareIntent,"News Bar Share using").setFlags(FLAG_ACTIVITY_NEW_TASK));



        }
    };

    View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            CustomViewHolder holder = (CustomViewHolder) view.getTag();
            int position = holder.getLayoutPosition();
            FeedMessage feedItem = listData.get(position);
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", feedItem.getLink());
            clipboard.setPrimaryClip(clip);
            String res = "Link copied in press-paper";
            Toast  toast = Toast.makeText(mContext, res, Toast.LENGTH_SHORT);
            toast.show();
            return false;
        }
    };

    public void updateData(List<FeedMessage> feedMessages) {
        Log.d(TAG_LOG, "updateData " );

        if (feedMessages != null) {
            listData.clear();
            listData.addAll(feedMessages);
        }
        else {
            listData = feedMessages;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return (null != listData ? listData.size() : 0);
    }

    public void animate(RecyclerView.ViewHolder viewHolder) {
        final Animation animAnticipateOvershoot = AnimationUtils.loadAnimation(mContext, R.anim.anticipate_overshoot_interpolator);
        viewHolder.itemView.setAnimation(animAnticipateOvershoot);
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {
        protected CardView cv;
        protected ImageView imageView;
        protected TextView titleView;
        protected TextView descView;
        protected TextView pubDateView;
        protected Button shareView;


        public CustomViewHolder(View view) {
            super(view);
            this.cv = (CardView) itemView.findViewById(R.id.cardView);
            this.imageView = (ImageView) view.findViewById(R.id.imageView);
            this.titleView = (TextView) view.findViewById(R.id.titleView);
            this.descView = (TextView) view.findViewById(R.id.descView);
            this.pubDateView = (TextView) view.findViewById(R.id.pubDateView);
            this.shareView = (Button) view.findViewById(R.id.shareView);

        }
    }

}