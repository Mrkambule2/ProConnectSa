package com.example.proconnectsa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proconnectsa.databinding.ActivityChatBinding;
import com.example.proconnectsa.models.Message;
import com.example.proconnectsa.network.NetworkClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private String currentUserId;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // In a real app, upload image to storage and get URL
                    sendMessage("[Image Attached]", "https://dummy-image-url.com/work-proof.jpg");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = DataManager.getInstance().getCurrentUser() != null ? 
                DataManager.getInstance().getCurrentUser().getId() : "anonymous";

        setSupportActionBar(binding.chatToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new MessageAdapter(messages);
        binding.recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMessages.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.editMessage.getText().toString();
            if (!text.isEmpty()) {
                sendMessage(text, null);
                binding.editMessage.setText("");
            }
        });

        binding.btnReplyOutside.setOnClickListener(v -> sendMessage("I'm outside.", null));
        binding.btnReplyLate.setOnClickListener(v -> sendMessage("I'm running 10 minutes late.", null));
        binding.btnReplyProof.setOnClickListener(v -> sendMessage("I've completed the work! Requesting job sign-off.", null));

        binding.btnAttach.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

        fetchMessages();
    }

    private void fetchMessages() {
        NetworkClient.getRetrofitClient().getMessages("timestamp.asc").enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messages.clear();
                    messages.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    binding.recyclerMessages.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String text, String imageUrl) {
        Message msg = new Message(currentUserId, text, System.currentTimeMillis());
        msg.setImageUrl(imageUrl);
        
        NetworkClient.getRetrofitClient().sendMessage(msg).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    fetchMessages();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void deleteMessage(Message msg, int position) {
        NetworkClient.getRetrofitClient().deleteMessage("eq." + msg.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    messages.remove(position);
                    adapter.notifyItemRemoved(position);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Message> list;
        MessageAdapter(List<Message> list) { this.list = list; }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new RecyclerView.ViewHolder(view) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message m = list.get(position);
            String display = m.getText();
            if (m.getImageUrl() != null) display += " 🖼️";
            ((TextView)holder.itemView.findViewById(android.R.id.text1)).setText(display);
            
            holder.itemView.setOnLongClickListener(v -> {
                if (m.getSenderId().equals(currentUserId)) {
                    deleteMessage(m, position);
                    return true;
                }
                return false;
            });
        }

        @Override
        public int getItemCount() { return list.size(); }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
