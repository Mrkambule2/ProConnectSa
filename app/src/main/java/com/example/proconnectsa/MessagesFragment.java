package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proconnectsa.databinding.FragmentMessagesBinding;
import com.example.proconnectsa.models.Message;
import com.example.proconnectsa.network.NetworkClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagesFragment extends Fragment {

    private FragmentMessagesBinding binding;
    private List<Message> conversationList = new ArrayList<>();
    private ConversationAdapter adapter;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (DataManager.getInstance().getCurrentUser() == null) {
            binding.textEmpty.setVisibility(View.VISIBLE);
            binding.textEmpty.setText("Please log in to see messages");
            return;
        }

        currentUserId = DataManager.getInstance().getCurrentUser().getId();
        adapter = new ConversationAdapter(conversationList);
        binding.recyclerMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerMessages.setAdapter(adapter);

        fetchConversations();
    }

    private Map<String, String> nameCache = new HashMap<>();

    @Override
    public void onResume() {
        super.onResume();
        fetchConversations();
    }

    private void fetchConversations() {
        if (currentUserId == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Filter for messages where current user is either sender or receiver
        // Using or=(sender_id.eq.UUID,receiver_id.eq.UUID)
        String filter = "(sender_id.eq." + currentUserId + ",receiver_id.eq." + currentUserId + ")";
        
        NetworkClient.getRetrofitClient().getMessages(filter, "created_at.desc").enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Message> allMessages = response.body();
                    
                    Map<String, Message> latestMessages = new HashMap<>();
                    List<String> idsToFetch = new ArrayList<>();

                    for (Message m : allMessages) {
                        String sId = m.getSenderId();
                        String rId = m.getReceiverId();
                        
                        if (sId == null) continue;
                        
                        String otherId;
                        if (sId.equals(currentUserId)) {
                            // I am the sender, other is receiver
                            otherId = rId;
                        } else {
                            // I am the receiver, other is sender
                            otherId = sId;
                        }

                        if (otherId == null) continue;
                        
                        if (!latestMessages.containsKey(otherId)) {
                            latestMessages.put(otherId, m);
                            if (!nameCache.containsKey(otherId) && !idsToFetch.contains(otherId)) {
                                idsToFetch.add(otherId);
                            }
                        }
                    }
                    
                    conversationList.clear();
                    conversationList.addAll(latestMessages.values());
                    
                    if (conversationList.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.textEmpty.setVisibility(View.VISIBLE);
                        binding.textEmpty.setText("No messages found");
                        adapter.notifyDataSetChanged();
                    } else {
                        fetchNamesAndRefresh(idsToFetch);
                    }
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                    String err = "Error " + response.code();
                    try { if (response.errorBody() != null) err = response.errorBody().string(); } catch (Exception ignored) {}
                    
                    if (err.contains("receiver_id")) {
                        Toast.makeText(getContext(), "Database update needed! Please add 'receiver_id' column to 'messages' table in Supabase.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Failed: " + err, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Map<String, String> avatarCache = new HashMap<>();

    private void fetchNamesAndRefresh(List<String> ids) {
        if (ids.isEmpty()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.textEmpty.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
            return;
        }

        final int[] remaining = {ids.size()};
        for (String id : ids) {
            NetworkClient.getRetrofitClient().getProfiles("eq." + id, null, null, null, "*").enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        Map<String, Object> profile = response.body().get(0);
                        String name = (String) profile.get("name");
                        String avatar = (String) profile.get("profile_image_url");
                        if (name != null) nameCache.put(id, name);
                        if (avatar != null) avatarCache.put(id, avatar);
                    }
                    checkFinished();
                }

                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    checkFinished();
                }

                private void checkFinished() {
                    remaining[0]--;
                    if (remaining[0] <= 0) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.textEmpty.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
        private List<Message> list;

        ConversationAdapter(List<Message> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Message m = list.get(position);
            String otherId = m.getSenderId().equals(currentUserId) ? m.getReceiverId() : m.getSenderId();
            
            String displayName = nameCache.containsKey(otherId) ? nameCache.get(otherId) : "User " + otherId.substring(0, 4);
            String avatarUrl = avatarCache.get(otherId);

            holder.txtName.setText(displayName);
            holder.txtLastMsg.setText(m.getText());
            
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                com.bumptech.glide.Glide.with(getContext()).load(avatarUrl).into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            try {
                // ISO 8601 to simple time
                java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(m.getTimestamp());
                if (date != null) {
                    holder.txtTime.setText(new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date));
                }
            } catch (Exception e) {
                holder.txtTime.setText("");
            }
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("receiver_id", otherId);
                intent.putExtra("receiver_name", displayName);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView imgAvatar;
            TextView txtName, txtLastMsg, txtTime;
            ViewHolder(View v) {
                super(v);
                imgAvatar = v.findViewById(R.id.img_avatar);
                txtName = v.findViewById(R.id.txt_name);
                txtLastMsg = v.findViewById(R.id.txt_last_message);
                txtTime = v.findViewById(R.id.txt_time);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}