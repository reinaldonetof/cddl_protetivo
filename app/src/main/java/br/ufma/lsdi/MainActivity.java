package br.ufma.lsdi;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.ConnectionFactory;
import br.ufma.lsdi.cddl.listeners.IConnectionListener;
import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.network.ConnectionImpl;
import br.ufma.lsdi.cddl.network.SecurityService;
import br.ufma.lsdi.cddl.pubsub.Publisher;
import br.ufma.lsdi.cddl.pubsub.PublisherFactory;
import br.ufma.lsdi.cddl.pubsub.Subscriber;
import br.ufma.lsdi.cddl.pubsub.SubscriberFactory;

public class MainActivity extends AppCompatActivity {

    CDDL cddl;
    private TextView messageTextView;
    private View sendButton;
    private ConnectionImpl con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setViews();

        if(!checkPermission()){
            requestPermission();
        }

        // Este codigo comentado envolve o uso seguro do CDDL. Estes métodos não devem ser chamados de forma sequencial.
        // Caso alguma equipe deseje usar o CDDL no modo seguro me procure que eu explico melhor (andre.cardoso@lsdi.ufma.br)
//        SecurityService securityService = new SecurityService(getApplicationContext());
//        securityService.generateCSR("andre.cardoso","LSDi","UFMA","SLZ","MA","BR","andre.cardoso@lsdi.ufma.br");
//        securityService.grantWritePermissionByCDDLTopic("andre.cardoso@lsdi.ufma.br", SecurityService.ALL_TOPICS);
//        securityService.grantReadPermissionByCDDLTopic("andre.cardoso@lsdi.ufma.br", SecurityService.ALL_TOPICS);
//        setCertificates("rootCA.crt", "client.crt", "ca_lsdi");

        initCDDL();
        subscribeMessage();
        sendButton.setOnClickListener(clickListener);
    }

    private void setViews() {
        sendButton = findViewById(R.id.sendButton);
        messageTextView = findViewById(R.id.messageTexView);
    }
    private void initCDDL() {
        String host = CDDL.startMicroBroker();
        //String host = CDDL.startSecureMicroBroker(getApplicationContext(), true);
        //val host = "broker.hivemq.com";
        con = ConnectionFactory.createConnection();
        con.setClientId("andre.cardoso@lsdi.ufma.br2");
        con.setHost(host);
        con.addConnectionListener(connectionListener);
        con.connect();
//        con.secureConnect(getApplicationContext());
        cddl = CDDL.getInstance();
        cddl.setConnection(con);
        cddl.setContext(this);
        cddl.startService();
        cddl.startCommunicationTechnology(CDDL.INTERNAL_TECHNOLOGY_ID);
        //cddl.startLocationSensor();
    }
    private void publishMessage() {
        Publisher publisher = PublisherFactory.createPublisher();
        publisher.addConnection(cddl.getConnection());
        MyMessage message = new MyMessage();
        message.setServiceName("Meu serviço");
        message.setServiceByteArray("Valor");
        publisher.publish(message);
    }
    private void subscribeMessage() {
        Subscriber sub = SubscriberFactory.createSubscriber();
        sub.addConnection(cddl.getConnection());
        sub.subscribeServiceByName("Meu serviço");
        //sub.subscribeServiceByName("Location");
        sub.setSubscriberListener(new ISubscriberListener() {
            @Override
            public void onMessageArrived(Message message) {
                if (message.getServiceName().equals("Meu serviço")) {
                    Log.d("_MAIN", "+++" + message);
                }
                Log.d("_MAIN", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>" + message);
            }
        });

    }

    private IConnectionListener connectionListener = new IConnectionListener() {
        @Override
        public void onConnectionEstablished() {
            messageTextView.setText("Conexão estabelecida.");
        }

        @Override
        public void onConnectionEstablishmentFailed() {
            messageTextView.setText("Falha na conexão.");
        }

        @Override
        public void onConnectionLost() {
            messageTextView.setText("Conexão perdida.");
        }

        @Override
        public void onDisconnectedNormally() {
            messageTextView.setText("Uma disconexão normal ocorreu.");
        }

    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            publishMessage();
        }
    };

    @Override
    protected void onDestroy() {
        cddl.stopLocationSensor();
        cddl.stopAllCommunicationTechnologies();
        cddl.stopService();
        con.disconnect();
        CDDL.stopMicroBroker();
        super.onDestroy();
    }

    private boolean checkPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int result = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
//                intent.putExtra(caFileName, caFileName);
//                intent.putExtra(certFileName, certFileName);
//                intent.putExtra(caAlias, caAlias);
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 1);
        }
    }

    private void setCertificates(String caFileName, String certFileName, String caAlias){
        if(!checkPermission()){
            requestPermission();
        }else{
            try{
                SecurityService securityService = new SecurityService(getApplicationContext());
                securityService.setCaCertificate(caFileName, caAlias);
                securityService.setCertificate(certFileName);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2296) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "All permissions are granted, you may import certificates!", Toast.LENGTH_SHORT).show();

//                    try{
//                        SecurityService securityService = new SecurityService(getApplicationContext());
//                        String caFileName = data.getStringExtra("caFileName");
//                        String certFileName = data.getStringExtra("certFileName");
//                        String caAlias = data.getStringExtra("caAlias");
//
//                        securityService.setCaCertificate(caFileName, caAlias);
//                        securityService.setCertificate(certFileName);
//                    }
//                    catch (Exception e){
//                        e.printStackTrace();
//                    }
                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}