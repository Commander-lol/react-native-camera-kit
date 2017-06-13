import React, {Component} from 'react';
import {
	requireNativeComponent,
	NativeModules,
	DeviceEventEmitter
} from 'react-native';
import { omit } from 'lodash'

const NativeCamera = requireNativeComponent('CameraView', null);
const NativeCameraModule = NativeModules.CameraModule;

const unnativeProps = ['onZoom', 'onMessage']

export default class CameraKitCamera extends React.Component {

	static async requestDeviceCameraAuthorization() {
		const usersAuthorizationAnswer = await NativeCameraModule.requestDeviceCameraAuthorization();
		return usersAuthorizationAnswer;
	}

	static async checkDeviceCameraAuthorizationStatus() {
		return await NativeCameraModule.checkDeviceCameraAuthorizationStatus();
	}

	static async hasCameraPermission() {
		const success = await NativeCameraModule.hasCameraPermission();
		return success;
	}

	componentWillMount() {
		this._addOnZoomChangedListener()
		this._addOnMessageListener()
	}

	componentWillUnmount() {
		this._removeOnZoomChangedListener()
		this._removeOnMessageListener()
	}

	render() {
		return <NativeCamera {...this.getNativeProps()}/>
	}

	getNativeProps() {
		return omit(this.props, unnativeProps)
	}

	async capture(saveToCameraRoll = true) {
		const imageTmpPath = await NativeCameraModule.capture(saveToCameraRoll);
		return imageTmpPath;
	}

	async changeCamera() {
		const success = await NativeCameraModule.changeCamera();
		return success;
	}

	async setFlashMode(flashMode = 'auto') {
		const success = await NativeCameraModule.setFlashMode(flashMode);
		return success;
	}

	_addOnZoomChangedListener(props) {
		const { onZoom } = props || this.props
		this._removeOnZoomChangedListener()
		if (onZoom) {
			this.zoomChangedListener = DeviceEventEmitter.addListener('ZoomComplete', this._onZoomChanged)
		}
	}

	_removeOnZoomChangedListener() {
		const listener = this.zoomChangedListener
		if (listener) {
			listener.remove()
		}
	}

	_addOnMessageListener(props) {
		const { onMessage } = props || this.props
		this._removeOnMessageListener()
		if (onMessage) {
			this.messageListener = DeviceEventEmitter.addListener('Message', this._onMessage)
		}
	}

	_removeOnMessageListener() {
		const listener = this.messageListener
		if (listener) {
			listener.remove()
		}
	}
	_onZoomChanged = (data) => {
		if (this.props.onZoom) {
			this.props.onZoom(data)
		}
	}
	_onMessage = (data) => {
		if (this.props.onMessage) {
			this.props.onMessage(data)
		}
	}
}
