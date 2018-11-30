package bytech.got2eat;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class Home extends AppCompatActivity implements AIListener{

    private Author user;
    private Author bot;
    private TextInputLayout userInput;
    private Button inputEnter;
    private List<Message> messages = new ArrayList<>();
    private AIDataService aiService;
    private AIRequest aiRequest;
    private Home thisInstance = this;
    private MessagesListAdapter<Message> adapter = new MessagesListAdapter<>(FirebaseAuth.getInstance().getUid(), null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        final AIConfiguration config = new AIConfiguration("bd09387ec42144bd9dbf3ea09141f6fd",
                AIConfiguration.SupportedLanguages.Portuguese,
                AIConfiguration.RecognitionEngine.System);

        aiService = new AIDataService(config);

        MessagesList messagesList = findViewById(R.id.messagesList);
        messagesList.setAdapter(adapter);

        userInput = findViewById(R.id.user_input_layout);

        inputEnter = findViewById(R.id.input_enter_button);
        inputEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Validate input
                final String message = userInput.getEditText().getText().toString();
                if (validateInput(message)==0){
                    Message messageObj = new Message(message, FirebaseAuth.getInstance().getUid(), new Date(), user);

                    messages.add(messageObj);
                    adapter.addToStart(messageObj, true);

                    //Send HTTP request to Dialogflow
                    aiRequest = new AIRequest();
                    aiRequest.setQuery(message);
                    new DialogTask(aiService, aiRequest, thisInstance).execute(aiRequest);
                    userInput.getEditText().setText("");

                }
            }
        });

        user = new Author(FirebaseAuth.getInstance().getUid(),FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),null);
        bot = new Author("bot", "bot", null);
    }

    private void respond(String message) {
        //Find if there are recipes
        String[] tokens = message.split("_");
        if (tokens.length > 1){
            //There is at least one recipe
            //Send initial
            Message initial = new Message(tokens[0], "bot", new Date(), bot);
            adapter.addToStart(initial, true);

            for (int i=1; i<tokens.length; i++){
                //Send separate messages and set their onClick to a recipe
                final Message obj = new Message(tokens[i], "bot", new Date(), bot);
                messages.add(obj);
                adapter.addToStart(obj, true);
                adapter.setOnMessageClickListener(new MessagesListAdapter.OnMessageClickListener<Message>() {
                    @Override
                    public void onMessageClick(Message message) {
                        if (message == obj){
                            Intent intent = new Intent(thisInstance, Recipe.class);
                            startActivity(intent);
                        }
                    }
                });
            }
        }
        else{
            Message obj = new Message(message, "bot", new Date(), bot);
            messages.add(obj);
            adapter.addToStart(obj, true);
        }
    }

    private int validateInput(String message){
        if (message.isEmpty()){
            userInput.setError(getString(R.string.empty_username));
            return 1;
        }
        else{
            userInput.setError(null);
            return 0;
        }
    }

    @Override
    public void onResult(AIResponse result) {
    }

    @Override
    public void onError(AIError error) {
    }

    @Override
    public void onAudioLevel(float level) {
    }

    @Override
    public void onListeningStarted() {
    }

    @Override
    public void onListeningCanceled() {
    }

    @Override
    public void onListeningFinished() {
    }

    private static class DialogTask extends AsyncTask<AIRequest,Void,AIResponse>{
        private AIDataService aiService;
        private AIRequest aiRequest;
        private static Home homeClass;

        DialogTask(AIDataService aiService, AIRequest aiRequest, Home homeClass){
            this.aiService = aiService;
            this.aiRequest = aiRequest;
            DialogTask.homeClass = homeClass;
        }

        @Override
        protected AIResponse doInBackground(AIRequest... aiRequests) {
            final AIRequest request = aiRequests[0];
            try {
                final AIResponse response = aiService.request(aiRequest);
                return response;
            } catch (AIServiceException e) {
            }
            return null;
        }
        @Override
        protected void onPostExecute(AIResponse response) {
            if (response != null) {

                Result result = response.getResult();
                String reply = result.getFulfillment().getSpeech();
                homeClass.respond(reply);
            }
        }
    }
}
