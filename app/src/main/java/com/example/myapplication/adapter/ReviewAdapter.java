package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.Review;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviews;
    private SimpleDateFormat dateFormat;

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public void updateReviews(List<Review> newReviews) {
        this.reviews = newReviews;
        notifyDataSetChanged();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        private TextView username;
        private TextView date;
        private RatingBar ratingBar;
        private TextView content;

        ReviewViewHolder(View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.reviewUsername);
            date = itemView.findViewById(R.id.reviewDate);
            ratingBar = itemView.findViewById(R.id.reviewRating);
            content = itemView.findViewById(R.id.reviewContent);
        }

        void bind(Review review) {
            username.setText(review.getUsername());
            date.setText(dateFormat.format(new Date(review.getTimestamp())));
            ratingBar.setRating(review.getRating());
            content.setText(review.getContent());
        }
    }
} 