var textToSend = document.getElementById("message").value = "";

function sendToDevice(){
    var toSend = document.getElementById("message").value;
    var firebaseRef = firebase.database().ref();
    
    firebaseRef.child("message").set(toSend);
}