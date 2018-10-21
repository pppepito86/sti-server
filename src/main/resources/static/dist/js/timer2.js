// Update the count down every 1 second
function showTimeToSubmit() {

    // Get todays date and time
    var now = new Date().getTime();
    
    // Find the distance between now an the count down date
    var distance = submitDate - now;
    if (distance <= 0) {
        document.getElementById("timetosubmit2").innerHTML = "";
        document.getElementById("timetosubmit3").innerHTML = "";
        return;
    }
    
    var seconds = Math.floor(distance / 1000) + 1;
    
    // Output the result in an element with id="demo"
    document.getElementById("timer2").innerHTML =  seconds;
    document.getElementById("timer3").innerHTML =  seconds;
    
    setTimeout(showTimeToSubmit, 1000);
};
showTimeToSubmit();