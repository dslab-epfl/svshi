from verifier.tracker import Message


class Verifier:

    def verify_write(self, message: Message):
        app = message.app_name
        device = message.device
        data = message.data
        print(app, device, data)

