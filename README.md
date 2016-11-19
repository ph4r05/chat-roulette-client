# Diffie-Hellman exercise - chat roulette client for PV181

Client communication library for chat-roulette algorithm demonstrating use 
of DH key agreement in practice.

## Introduction

For the client-server communication take a look at the [chat-roulette-python] 
repository readme, it contains brief protocol overview. 

The basic communication with the server is already handled in this client 
repository so you don't have to take care about ping protocol. 

Feel free to use existing code snippets to send missing messages and finish the 
homework assignment.

## Protocol

All the binary values are in the protocol encoded in Base64. To convert 
`byte[]` to Base64 string, use for example `DatatypeConverter.printBase64Binary(res);`.
For reverse conversion use `DatatypeConverter.parseBase64Binary(theString);`.

On successfull pairing, the server sends you a message

```json
{"uco":"1","cmd":"pair"}
````

Then initiator starts the protocol by sending his DH public key part to the responder:

```json
{"uco":"1","sub":"dh1","session":"alpha","dhpub":"MIIBpjCCARsGCSqGSIb3DQEDATCCAQwCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoCAgIAA4GEAAKBgF28kIT49kNTZe3L6MP0xi7oSsxlZF+5kO70QXEkZ2fa89xuNghKJdq8B0ySHQKo+G+btpGb25B/TcZgj/FwbguwPDQkZgWcC4UwCXeWTpgQe0H6kmqnyR4SusrdMAAa1qFmiBNiYdUGo5ZRQ+GX69nJoYFdbRAwLKoQ2jonn4yx","cmd":"comm","nonce":-235611940}
```

This is already implemented in the code so you see how to handle incoming messages
and send new ones. The message above was sent by the following code:

```java
JSONObject dh1 = preparePeerMsg(SUB_DH1);
dh1.put(FIELD_DHPUB, pubKeyBase64);
comm.enqueue(dh1);
```

The messages are simple JSON objects, `preparePeerMsg` prepares a new JSON message 
skeleton for you (fills in required fields like `uco, session, nonce, cmd`) and sets `sub` field to the value
`SUB_DH1` - meaning this is the first DH message (from initiator to responder).
The public key is set to the message, to the field with name `FIELD_DHPUB` and
message is enqueued to the sending queue.

After responder receives the DH1, he replies with DH2 message (containing his DH pub key):

```json
{"uco":"325219","sub":"dh2","session":"alpha","cmd":"comm","dhpub":"MIIBpjCCARsGCSqGSIb3DQEDATCCAQwCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoCAgIAA4GEAAKBgFYrv6THBfdntqwmw0G+ZFQesdn2kgX0v5HFsNfMQPoEOMjNa8GKfPmSlZFsRCAoTdeEr3x8/72SKRpf6tDe03lU2bqITMTdDWIp1ToH1oIiAGqdPLg9/9Um5648iLugCjJRPstItLzxQtpZ1h7+OWuExdX+ark3pbp8SS8KQDtw","nonce":2107416065}
```

After DH key agreement finishes on both sides they has to verify they have the same shared secret key.
This is done by exchanging `dhc` message (`SUB_DH_CONF`) with the field `dhverif` (`FIELD_DHVERIF`)
containing the result of the following operation:

```
HmacSHA1(secretKey, getConnectionId().getBytes())
```

In other words - they verify the secret key by HMAC-ing (using HmacSHA1, `Mac` engine class)
the connection string, using the secret key. Note: we cannot HMAC string directly,
so we need to convert it to byte array, thats why we are hmacing `getConnectionId().getBytes()`.

The side which receives DH2 message sends the verification message `SUB_DH_CONF` first.

The side which receives the `SUB_DH_CONF` verification message is supposed to compute HMAC on his own 
and compare it with the given value. Do it in such a way it does not leak infromation
by time side channel (constant time array comparison).

Then it sends back his own `SUB_DH_CONF` message with his own HMAC (base64 encoded).

The final message looks like this:

```json
{"uco":"1","sub":"dhc","session":"alpha","dhverif":"Vk7Nz8peqNAYG2R1iXLbhbHHzao=","cmd":"comm","nonce":1749921803}
```

The protocol finishes by each side receiving `SUB_DH_CONF` verification message.


[chat-roulette-python]: https://github.com/ph4r05/chat-roulette-python
