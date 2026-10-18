package fr.neamar.kiss.broadcast;

import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;

@RequiresApi(api = Build.VERSION_CODES.N)
public class IncomingCallScreeningService extends CallScreeningService {

    @Override
    public void onScreenCall(@NonNull Call.Details callDetails) {
        respondToCall(callDetails, new CallResponse.Builder().build());

        if (callDetails.getHandle() != null) {
            String phoneNumber = callDetails.getHandle().getSchemeSpecificPart();
            DataHandler dataHandler = KissApplication.getApplication(this).getDataHandler();
            dataHandler.addPhoneNumberToHistory(phoneNumber);
        }
    }
}
