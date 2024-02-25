function sendMessage() {
    var messageInput = document.getElementById('messageInput');
    var message = messageInput.value;
    messageInput.value = ''; // Clear the input field
    var chatContainer = document.querySelector('.chat-container');
    
    var userMessageDiv = document.createElement('div');
    userMessageDiv.className = 'user-message';
    userMessageDiv.innerText = message;
    chatContainer.appendChild(userMessageDiv);
    
    var botMessageDiv = document.createElement('div');
    botMessageDiv.className = 'bot-message';
    chatContainer.appendChild(botMessageDiv);
    
    var eventSource = new EventSource('/chat/send-message?message=' + message + "&chatId=" + chatId);
    eventSource.onmessage = function(event) {
        if (event.data == "$CLOSE") {
            eventSource.close();
        } else {
            var token = event.data.replace("$SPACE", " ");
            botMessageDiv.innerText += token;
        }
    };
}
