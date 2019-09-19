//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
import React, { Component } from 'react';
import Permissions from 'react-native-permissions';
import {
  Button,
  Platform,
  StyleSheet,
  Text,
  View
} from 'react-native';
import {
  NativeEventEmitter,
  NativeModules
} from 'react-native';

const ChirpSDK = NativeModules.ChirpSDK;
const ChirpSDKEmitter = new NativeEventEmitter(ChirpSDK);

const key = '<YOUR_CHIRP_APP_KEY>';
const secret = '<YOUR_CHIRP_APP_SECRET>';
const config = '<YOUR_CHIRP_APP_CONFIG>';


export default class App extends Component<{}> {

  constructor(props) {
    super(props);
    this.state = {
      'initialised': false,
      'status': 'Sleeping',
      'data': '----------'
    }
  }

  async componentDidMount() {
    const response = await Permissions.check('microphone')
    if (response !== 'authorized') {
      await Permissions.request('microphone')
    }

    this.onStateChanged = ChirpSDKEmitter.addListener(
      'onStateChanged',
      (event) => {
        if (event.status === ChirpSDK.CHIRP_SDK_STATE_STOPPED) {
          this.setState({ status: 'Stopped' });
        } else if (event.status === ChirpSDK.CHIRP_SDK_STATE_RUNNING) {
          this.setState({ status: 'Running' });
        } else if (event.status === ChirpSDK.CHIRP_SDK_STATE_SENDING) {
          this.setState({ status: 'Sending' });
        } else if (event.status === ChirpSDK.CHIRP_SDK_STATE_RECEIVING) {
          this.setState({ status: 'Receiving' });
        }
      }
    );

    this.onReceived = ChirpSDKEmitter.addListener(
      'onReceived',
      (event) => {
        if (event.data.length) {
          this.setState({ data: event.data });
        }
      }
    )

    this.onError = ChirpSDKEmitter.addListener(
      'onError', (event) => { console.warn(event.message) }
    )

    try {
      ChirpSDK.init(key, secret);
      ChirpSDK.setConfig(config);
      ChirpSDK.start();
      this.setState({ initialised: true })
    } catch(e) {
      console.warn(e.message);
    }
  }

  componentWillUnmount() {
    this.onStateChanged.remove();
    this.onReceived.remove();
    this.onError.remove();
  }

  onPress() {
    ChirpSDK.send([0, 1, 2, 3, 4]);
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to Chirp!
        </Text>
        <Text style={styles.instructions}>
          {this.state.status}
        </Text>
        <Text style={styles.instructions}>
          {this.state.data}
        </Text>
        <Button
          onPress={this.onPress}
          title='SEND'
          disabled={!this.state.initialised}
        />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 60,
  },
  instructions: {
    padding: 10,
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
