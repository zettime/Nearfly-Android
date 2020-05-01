package de.pbma.nearbyconnections;

class ReceiveWithProgressCallback /*extends PayloadCallback */{
//    private final SimpleArrayMap<Long, NotificationCompat.Builder> incomingPayloads =
//            new SimpleArrayMap<>();
//    private final SimpleArrayMap<Long, NotificationCompat.Builder> outgoingPayloads =
//            new SimpleArrayMap<>();
//    private Context context;
//
//    NotificationManager notificationManager;
//
//
//    public ReceiveWithProgressCallback(Context context){
//        this.context = context;
//        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//    }
//
//    private void sendPayload(String endpointId, Payload payload) {
//        if (payload.getType() == Payload.Type.BYTES) {
//            // No need to track progress for bytes.
//            return;
//        }
//
//        // Build and start showing the notification.
//        NotificationCompat.Builder notification = buildNotification(payload, /*isIncoming=*/ false);
//        notificationManager.notify((int) payload.getId(), notification.build());
//
//        // Add it to the tracking list so we can update it.
//        outgoingPayloads.put(payload.getId(), notification);
//    }
//
//    private NotificationCompat.Builder buildNotification(Payload payload, boolean isIncoming) {
//        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
//                .setContentTitle(isIncoming ? "Receiving..." : "Sending...")
//                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_focused);
//
//        boolean indeterminate = false;
//        if (payload.getType() == Payload.Type.STREAM) {
//            // We can only show indeterminate progress for stream payloads.
//            indeterminate = true;
//        }
//        notification.setProgress(100, 0, indeterminate);
//        return notification;
//    }
//
//    @Override
//    public void onPayloadReceived(String endpointId, Payload payload) {
//        if (payload.getType() == Payload.Type.BYTES) {
//            // No need to track progress for bytes.
//            return;
//        }
//
//        // Build and start showing the notification.
//        NotificationCompat.Builder notification = buildNotification(payload, true /*isIncoming*/);
//        notificationManager.notify((int) payload.getId(), notification.build());
//
//        // Add it to the tracking list so we can update it.
//        incomingPayloads.put(payload.getId(), notification);
//    }
//
//    @Override
//    public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
//        long payloadId = update.getPayloadId();
//        NotificationCompat.Builder notification = null;
//        if (incomingPayloads.containsKey(payloadId)) {
//            notification = incomingPayloads.get(payloadId);
//            if (update.getStatus() != PayloadTransferUpdate.Status.IN_PROGRESS) {
//                // This is the last update, so we no longer need to keep track of this notification.
//                incomingPayloads.remove(payloadId);
//            }
//        } else if (outgoingPayloads.containsKey(payloadId)) {
//            notification = outgoingPayloads.get(payloadId);
//            if (update.getStatus() != PayloadTransferUpdate.Status.IN_PROGRESS) {
//                // This is the last update, so we no longer need to keep track of this notification.
//                outgoingPayloads.remove(payloadId);
//            }
//        }
//
//        if (notification == null) {
//            return;
//        }
//
//        switch (update.getStatus()) {
//            case PayloadTransferUpdate.Status.IN_PROGRESS:
//                long size = update.getTotalBytes();
//                if (size == -1) {
//                    // This is a stream payload, so we don't need to update anything at this point.
//                    return;
//                }
//                int percentTransferred =
//                        (int) (100.0 * (update.getBytesTransferred() / (double) update.getTotalBytes()));
//                notification.setProgress(100, percentTransferred, /* indeterminate= */ false);
//                Log.v(Constants.TAG, "in Progress");
//                break;
//            case PayloadTransferUpdate.Status.SUCCESS:
//                // SUCCESS always means that we transferred 100%.
//                notification
//                        .setProgress(100, 100, /* indeterminate= */ false)
//                        .setContentText("Transfer complete!");
//                break;
//            case PayloadTransferUpdate.Status.FAILURE:
//            case PayloadTransferUpdate.Status.CANCELED:
//                notification.setProgress(0, 0, false).setContentText("Transfer failed");
//                break;
//            default:
//                // Unknown status.
//        }
//
//        notificationManager.notify((int) payloadId, notification.build());
//    }
}

