package com.smartcalendar.ai.ui.chat;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.smartcalendar.ai.R;
import com.smartcalendar.ai.util.SystemBars;
import com.smartcalendar.ai.viewmodel.ChatViewModel;

public class ChatActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private RecyclerView rvChat;
    private EditText etMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        SystemBars.apply(this, findViewById(R.id.appBar), findViewById(R.id.content));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        toolbar.inflateMenu(R.menu.menu_chat);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_new_chat) {
                viewModel.newConversation();
                return true;
            }
            return false;
        });

        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);
        TextView tvTyping = findViewById(R.id.tvTyping);

        ChatAdapter adapter = new ChatAdapter(new ChatAdapter.Listener() {
            @Override
            public void onSave(ChatMessage message) {
                viewModel.confirmEvent(message.id);
            }

            @Override
            public void onDiscard(ChatMessage message) {
                viewModel.discardEvent(message.id);
            }

            @Override
            public void onPick(ChatMessage message, int candidateIndex) {
                viewModel.selectCandidate(message.id, candidateIndex);
            }
        });
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        viewModel.getMessages().observe(this, list ->
                adapter.submitList(list, () -> {
                    if (adapter.getItemCount() > 0) {
                        rvChat.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }));

        viewModel.getLoading().observe(this, loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            tvTyping.setVisibility(busy ? View.VISIBLE : View.GONE);
            btnSend.setEnabled(!busy);
            btnSend.setAlpha(busy ? 0.5f : 1f);
        });

        btnSend.setOnClickListener(v -> sendCurrentMessage());
        etMessage.setOnEditorActionListener((v, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage();
                return true;
            }
            return false;
        });
    }

    private void sendCurrentMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        etMessage.setText("");
        viewModel.send(text);
    }
}
