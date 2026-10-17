package com.example.proconnectsa;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proconnectsa.databinding.ActivityChatBinding;
import com.example.proconnectsa.models.Message;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.network.NetworkClient;
import com.example.proconnectsa.utils.UIUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private String currentUserId;
    private String receiverId;
    private String receiverName;

    private android.os.Handler pollHandler = new android.os.Handler();
    private Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            fetchMessages();
            pollHandler.postDelayed(this, 3000); // Poll every 3 seconds
        }
    };

    private MediaRecorder recorder;
    private String audioFileName;
    private boolean isRecording = false;
    private MediaPlayer player;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    android.graphics.Bitmap imageBitmap = (android.graphics.Bitmap) extras.get("data");
                    uploadImage(imageBitmap);
                }
            }
    );

    private final ActivityResultLauncher<Intent> locationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    double lat = result.getData().getDoubleExtra("latitude", 0);
                    double lng = result.getData().getDoubleExtra("longitude", 0);
                    sendLocation(lat, lng);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        User user = DataManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) {
            Toast.makeText(this, "Session lost. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getId();
        receiverId = getIntent().getStringExtra("receiver_id");
        receiverName = getIntent().getStringExtra("receiver_name");

        if (receiverId == null) {
            Toast.makeText(this, "Chat Error: No recipient ID found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (receiverName != null) {
            setTitle("Chat with " + receiverName);
        } else {
            setTitle("Chat");
        }

        setSupportActionBar(binding.chatToolbar);
        binding.chatToolbar.setNavigationOnClickListener(v -> finish());

        adapter = new MessageAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerMessages.setLayoutManager(layoutManager);
        binding.recyclerMessages.setAdapter(adapter);

        binding.recyclerMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && !messages.isEmpty()) {
                binding.recyclerMessages.postDelayed(() -> 
                    binding.recyclerMessages.smoothScrollToPosition(messages.size() - 1), 100);
            }
        });

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.editMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text, null, null, null, null);
                binding.editMessage.setText("");
            }
        });

        binding.btnReplyOutside.setOnClickListener(v -> sendMessage("I'm outside.", null, null, null, null));
        binding.btnReplyLate.setOnClickListener(v -> sendMessage("I'm running 10 minutes late.", null, null, null, null));
        binding.btnReplyProof.setOnClickListener(v -> sendMessage("I've completed the work! Requesting job sign-off.", null, null, null, null));

        binding.btnAttach.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

        binding.btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        binding.btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra("pick_mode", true);
            locationLauncher.launch(intent);
        });

        fetchMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 200);
            return;
        }

        audioFileName = getExternalCacheDir().getAbsolutePath() + "/audiorecord.3gp";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(audioFileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            binding.btnRecord.setIcon(getDrawable(android.R.drawable.ic_media_pause));
            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("ChatActivity", "prepare() failed");
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                Log.e("ChatActivity", "Stop recording failed", e);
            }
            recorder.release();
            recorder = null;
            isRecording = false;
            binding.btnRecord.setIcon(getDrawable(android.R.drawable.ic_btn_speak_now));
            uploadAudio();
        }
    }

    private void uploadImage(android.graphics.Bitmap bitmap) {
        UIUtils.getInstance().showProcessing(this, "Sending image...");
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] data = baos.toByteArray();

        String fileName = "chat_" + System.currentTimeMillis() + ".jpg";
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), data);

        NetworkClient.getRetrofitClient().uploadAvatar(fileName, requestBody).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    String url = "https://tyyexrmomanepliljnjp.supabase.co/storage/v1/object/public/avatars/" + fileName;
                    sendMessage("[Image]", url, null, null, null);
                    UIUtils.getInstance().showSuccess("Image Sent", null);
                } else {
                    UIUtils.getInstance().showError("Failed", "Upload error");
                }
            }
            @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {
                UIUtils.getInstance().showError("Error", t.getMessage());
            }
        });
    }

    private void uploadAudio() {
        File file = new File(audioFileName);
        if (!file.exists()) return;
        
        UIUtils.getInstance().showProcessing(this, "Sending voice note...");
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(bytes);
        } catch (IOException e) {
            UIUtils.getInstance().showError("Error", "Could not read audio file");
            return;
        }

        String fileName = "voice_" + System.currentTimeMillis() + ".3gp";
        RequestBody requestBody = RequestBody.create(MediaType.parse("audio/3gpp"), bytes);

        NetworkClient.getRetrofitClient().uploadAvatar(fileName, requestBody).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    String url = "https://tyyexrmomanepliljnjp.supabase.co/storage/v1/object/public/avatars/" + fileName;
                    sendMessage("[Voice Note]", null, url, null, null);
                    UIUtils.getInstance().showSuccess("Sent", null);
                } else {
                    UIUtils.getInstance().showError("Failed", "Voice upload error");
                }
            }
            @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {
                UIUtils.getInstance().showError("Error", t.getMessage());
            }
        });
    }

    private void sendLocation(double lat, double lng) {
        sendMessage("[Location Shared]", null, null, lat, lng);
    }

    private void fetchMessages() {
        if (receiverId == null) return;

        String orFilter = "(and(sender_id.eq." + currentUserId + ",receiver_id.eq." + receiverId + ")," +
                          "and(sender_id.eq." + receiverId + ",receiver_id.eq." + currentUserId + "))";

        NetworkClient.getRetrofitClient().getMessages(orFilter, "created_at.asc").enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Message> newMessages = response.body();
                    
                    // Always update if the size changed, or if there's a "Sending..." message
                    // that needs to be replaced with real data from server
                    boolean hasPending = false;
                    for (Message m : messages) {
                        if ("Sending...".equals(m.getTimestamp())) {
                            hasPending = true;
                            break;
                        }
                    }

                    if (newMessages.size() != messages.size() || hasPending) {
                        messages.clear();
                        messages.addAll(newMessages);
                        adapter.notifyDataSetChanged();
                        if (!messages.isEmpty()) {
                            binding.recyclerMessages.scrollToPosition(messages.size() - 1);
                        }
                    }
                } else if (response.code() == 404) {
                    Toast.makeText(ChatActivity.this, "DB Error: 'messages' table not found. Run SQL fix.", Toast.LENGTH_LONG).show();
                } else {
                    String error = "Error " + response.code();
                    try { if (response.errorBody() != null) error = response.errorBody().string(); } catch (Exception ignored) {}
                    if (error.contains("receiver_id")) {
                        Toast.makeText(ChatActivity.this, "DB Error: Column 'receiver_id' is missing. Run SQL: ALTER TABLE messages ADD COLUMN receiver_id UUID REFERENCES auth.users(id);", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ChatActivity.this, "Load Error: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e("ChatActivity", "Connection: " + t.getMessage());
            }
        });
    }

    private void sendMessage(String text, String imageUrl, String audioUrl, Double lat, Double lng) {
        if (currentUserId == null || receiverId == null) {
            String msg = "IDs Missing: Me=" + currentUserId + ", Them=" + receiverId;
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            Log.e("ChatActivity", msg);
            return;
        }
        
        binding.editMessage.setText("");
        
        // WhatsApp style: Add to list immediately with a temporary local message
        Message localMsg = new Message(currentUserId, receiverId, text, "Sending...");
        localMsg.setImageUrl(imageUrl);
        localMsg.setAudioUrl(audioUrl);
        localMsg.setLatitude(lat);
        localMsg.setLongitude(lng);
        
        messages.add(localMsg);
        adapter.notifyItemInserted(messages.size() - 1);
        binding.recyclerMessages.scrollToPosition(messages.size() - 1);

        // Raw map to avoid any extra fields that might cause 23502/400 errors
        Map<String, Object> msgMap = new java.util.HashMap<>();
        msgMap.put("sender_id", currentUserId);
        msgMap.put("receiver_id", receiverId);
        msgMap.put("text", text);
        if (imageUrl != null) msgMap.put("image_url", imageUrl);
        if (audioUrl != null) msgMap.put("audio_url", audioUrl);
        if (lat != null) msgMap.put("latitude", lat);
        if (lng != null) msgMap.put("longitude", lng);

        List<Map<String, Object>> list = new ArrayList<>();
        list.add(msgMap);

        Log.d("ChatActivity", "Inserting message: " + msgMap.toString());

        NetworkClient.getRetrofitClient().sendMessageRaw(list).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful()) {
                    fetchMessages(); 
                } else {
                    String error = "Error " + response.code();
                    try { if (response.errorBody() != null) error = response.errorBody().string(); } catch (Exception ignored) {}
                    
                    Log.e("ChatActivity", "Insert failed: " + error);
                    Toast.makeText(ChatActivity.this, "Send Failed: Database error", Toast.LENGTH_SHORT).show();
                    
                    messages.remove(localMsg);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e("ChatActivity", "Network error", t);
                Toast.makeText(ChatActivity.this, "Connection Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Message> list;
        private static final int TYPE_SENT = 1;
        private static final int TYPE_RECEIVED = 2;

        MessageAdapter(List<Message> list) { this.list = list; }

        @Override
        public int getItemViewType(int position) {
            Message m = list.get(position);
            if (m.getSenderId() == null) return TYPE_RECEIVED;
            return m.getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_SENT) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            }
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message m = list.get(position);
            MessageViewHolder vh = (MessageViewHolder) holder;

            vh.text.setText(m.getText());
            
            String timeStr = "";
            try {
                // Parse ISO 8601 string from Supabase
                Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(m.getTimestamp());
                if (date != null) {
                    timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
                }
            } catch (Exception e) {
                timeStr = "--:--";
            }
            vh.time.setText(timeStr);

            if (m.getImageUrl() != null && !m.getImageUrl().isEmpty()) {
                vh.image.setVisibility(View.VISIBLE);
                Glide.with(ChatActivity.this).load(m.getImageUrl()).into(vh.image);
            } else {
                vh.image.setVisibility(View.GONE);
            }

            if (m.getAudioUrl() != null && !m.getAudioUrl().isEmpty()) {
                vh.playAudio.setVisibility(View.VISIBLE);
                vh.playAudio.setOnClickListener(v -> playAudio(m.getAudioUrl()));
            } else {
                vh.playAudio.setVisibility(View.GONE);
            }

            if (m.getLatitude() != null && m.getLongitude() != null) {
                vh.viewLocation.setVisibility(View.VISIBLE);
                vh.viewLocation.setOnClickListener(v -> {
                    String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(Location)", m.getLatitude(), m.getLongitude(), m.getLatitude(), m.getLongitude());
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                });
            } else {
                vh.viewLocation.setVisibility(View.GONE);
            }

            holder.itemView.setOnLongClickListener(v -> {
                if (m.getSenderId().equals(currentUserId)) {
                    deleteMessage(m, position);
                    return true;
                }
                return false;
            });
        }

        private void playAudio(String url) {
            if (player != null) {
                player.release();
            }
            player = new MediaPlayer();
            try {
                player.setDataSource(url);
                player.prepare();
                player.start();
                Toast.makeText(ChatActivity.this, "Playing audio...", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(ChatActivity.this, "Error playing audio", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView text, time;
            ImageView image;
            View playAudio, viewLocation;
            MessageViewHolder(View v) {
                super(v);
                text = v.findViewById(R.id.msg_text);
                time = v.findViewById(R.id.msg_time);
                image = v.findViewById(R.id.msg_image);
                playAudio = v.findViewById(R.id.btn_play_audio);
                viewLocation = v.findViewById(R.id.btn_view_location);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
        if (recorder != null) recorder.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_close_chat) {
            finish();
            return true;
        } else if (id == R.id.action_dashboard) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
