using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;

[assembly: CompilationRelaxations(8)]
[assembly: RuntimeCompatibility(WrapNonExceptionThrows = true)]
[assembly: Debuggable(DebuggableAttribute.DebuggingModes.IgnoreSymbolStoreSequencePoints)]
[assembly: AssemblyTitle("ModbusTCP")]
[assembly: AssemblyDescription("")]
[assembly: AssemblyConfiguration("")]
[assembly: AssemblyCompany("BuR")]
[assembly: AssemblyProduct("ModbusTCP")]
[assembly: AssemblyCopyright("Copyright ©  2009")]
[assembly: AssemblyTrademark("")]
[assembly: ComVisible(false)]
[assembly: Guid("a6057265-c585-4e57-89f9-613c7dccd52d")]
[assembly: AssemblyFileVersion("3.3")]
[assembly: AssemblyVersion("3.3.0.0")]
namespace ModbusTCP;

/// <summary>
/// Modbus TCP common driver class. This class implements a modbus TCP master driver.
/// It supports the following commands:
///
/// Read coils
/// Read discrete inputs
/// Write single coil
/// Write multiple cooils
/// Read holding register
/// Read input register
/// Write single register
/// Write multiple register
///
/// All commands can be sent in synchronous or asynchronous mode. If a value is accessed
/// in synchronous mode the program will stop and wait for slave to response. If the 
/// slave didn't answer within a specified time a timeout exception is called.
/// The class uses multi threading for both synchronous and asynchronous access. For
/// the communication two lines are created. This is necessary because the synchronous
/// thread has to wait for a previous command to finish.
///
/// </summary>
public class Master
{
	/// <summary>Response data event. This event is called when new data arrives</summary>
	public delegate void ResponseData(ushort id, byte unit, byte function, byte[] data);

	/// <summary>Exception data event. This event is called when the data is incorrect</summary>
	public delegate void ExceptionData(ushort id, byte unit, byte function, byte exception);

	private const byte fctReadCoil = 1;

	private const byte fctReadDiscreteInputs = 2;

	private const byte fctReadHoldingRegister = 3;

	private const byte fctReadInputRegister = 4;

	private const byte fctWriteSingleCoil = 5;

	private const byte fctWriteSingleRegister = 6;

	private const byte fctWriteMultipleCoils = 15;

	private const byte fctWriteMultipleRegister = 16;

	private const byte fctReadWriteMultipleRegister = 23;

	/// <summary>Constant for exception illegal function.</summary>
	public const byte excIllegalFunction = 1;

	/// <summary>Constant for exception illegal data address.</summary>
	public const byte excIllegalDataAdr = 2;

	/// <summary>Constant for exception illegal data value.</summary>
	public const byte excIllegalDataVal = 3;

	/// <summary>Constant for exception slave device failure.</summary>
	public const byte excSlaveDeviceFailure = 4;

	/// <summary>Constant for exception acknowledge.</summary>
	public const byte excAck = 5;

	/// <summary>Constant for exception slave is busy/booting up.</summary>
	public const byte excSlaveIsBusy = 6;

	/// <summary>Constant for exception gate path unavailable.</summary>
	public const byte excGatePathUnavailable = 10;

	/// <summary>Constant for exception not connected.</summary>
	public const byte excExceptionNotConnected = 253;

	/// <summary>Constant for exception connection lost.</summary>
	public const byte excExceptionConnectionLost = 254;

	/// <summary>Constant for exception response timeout.</summary>
	public const byte excExceptionTimeout = byte.MaxValue;

	/// <summary>Constant for exception wrong offset.</summary>
	private const byte excExceptionOffset = 128;

	/// <summary>Constant for exception send failt.</summary>
	private const byte excSendFailt = 100;

	private static ushort _timeout = 500;

	private static ushort _refresh = 10;

	private static bool _connected = false;

	private Socket tcpAsyCl;

	private byte[] tcpAsyClBuffer = new byte[2048];

	private Socket tcpSynCl;

	private byte[] tcpSynClBuffer = new byte[2048];

	/// <summary>Response timeout. If the slave didn't answers within in this time an exception is called.</summary>
	/// <value>The default value is 500ms.</value>
	public ushort timeout
	{
		get
		{
			return _timeout;
		}
		set
		{
			_timeout = value;
		}
	}

	/// <summary>Refresh timer for slave answer. The class is polling for answer every X ms.</summary>
	/// <value>The default value is 10ms.</value>
	public ushort refresh
	{
		get
		{
			return _refresh;
		}
		set
		{
			_refresh = value;
		}
	}

	/// <summary>Shows if a connection is active.</summary>
	public bool connected => _connected;

	/// <summary>Response data event. This event is called when new data arrives</summary>
	public event ResponseData OnResponseData;

	/// <summary>Exception data event. This event is called when the data is incorrect</summary>
	public event ExceptionData OnException;

	/// <summary>Create master instance without parameters.</summary>
	public Master()
	{
	}

	/// <summary>Create master instance with parameters.</summary>
	/// <param name="ip">IP adress of modbus slave.</param>
	/// <param name="port">Port number of modbus slave. Usually port 502 is used.</param>
	public Master(string ip, ushort port)
	{
		connect(ip, port);
	}

	/// <summary>Start connection to slave.</summary>
	/// <param name="ip">IP adress of modbus slave.</param>
	/// <param name="port">Port number of modbus slave. Usually port 502 is used.</param>
	public void connect(string ip, ushort port)
	{
		try
		{
			if (!IPAddress.TryParse(ip, out IPAddress _))
			{
				ip = Dns.GetHostEntry(ip).AddressList[0].ToString();
			}
			tcpAsyCl = new Socket(IPAddress.Parse(ip).AddressFamily, SocketType.Stream, ProtocolType.Tcp);
			tcpAsyCl.Connect(new IPEndPoint(IPAddress.Parse(ip), port));
			tcpAsyCl.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.SendTimeout, _timeout);
			tcpAsyCl.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReceiveTimeout, _timeout);
			tcpAsyCl.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.Debug, 1);
			tcpSynCl = new Socket(IPAddress.Parse(ip).AddressFamily, SocketType.Stream, ProtocolType.Tcp);
			tcpSynCl.Connect(new IPEndPoint(IPAddress.Parse(ip), port));
			tcpSynCl.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.SendTimeout, _timeout);
			tcpSynCl.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReceiveTimeout, _timeout);
			tcpSynCl.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.Debug, 1);
			_connected = true;
		}
		catch (IOException ex)
		{
			_connected = false;
			throw ex;
		}
	}

	/// <summary>Stop connection to slave.</summary>
	public void disconnect()
	{
		Dispose();
	}

	/// <summary>Destroy master instance.</summary>
	~Master()
	{
		Dispose();
	}

	/// <summary>Destroy master instance</summary>
	public void Dispose()
	{
		if (tcpAsyCl != null)
		{
			if (tcpAsyCl.Connected)
			{
				try
				{
					tcpAsyCl.Shutdown(SocketShutdown.Both);
				}
				catch
				{
				}
				tcpAsyCl.Close();
			}
			tcpAsyCl = null;
		}
		if (tcpSynCl == null)
		{
			return;
		}
		if (tcpSynCl.Connected)
		{
			try
			{
				tcpSynCl.Shutdown(SocketShutdown.Both);
			}
			catch
			{
			}
			tcpSynCl.Close();
		}
		tcpSynCl = null;
	}

	internal void CallException(ushort id, byte unit, byte function, byte exception)
	{
		if (tcpAsyCl != null && tcpSynCl != null)
		{
			if (exception == 254)
			{
				tcpSynCl = null;
				tcpAsyCl = null;
			}
			if (this.OnException != null)
			{
				this.OnException(id, unit, function, exception);
			}
		}
	}

	internal static ushort SwapUInt16(ushort inValue)
	{
		return (ushort)(((inValue & 0xFF00) >> 8) | ((inValue & 0xFF) << 8));
	}

	/// <summary>Read coils from slave asynchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	public void ReadCoils(ushort id, byte unit, ushort startAddress, ushort numInputs)
	{
		WriteAsyncData(CreateReadHeader(id, unit, startAddress, numInputs, 1), id);
	}

	/// <summary>Read coils from slave synchronous.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	/// <param name="values">Contains the result of function.</param>
	public void ReadCoils(ushort id, byte unit, ushort startAddress, ushort numInputs, ref byte[] values)
	{
		values = WriteSyncData(CreateReadHeader(id, unit, startAddress, numInputs, 1), id);
	}

	/// <summary>Read discrete inputs from slave asynchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	public void ReadDiscreteInputs(ushort id, byte unit, ushort startAddress, ushort numInputs)
	{
		WriteAsyncData(CreateReadHeader(id, unit, startAddress, numInputs, 2), id);
	}

	/// <summary>Read discrete inputs from slave synchronous.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	/// <param name="values">Contains the result of function.</param>
	public void ReadDiscreteInputs(ushort id, byte unit, ushort startAddress, ushort numInputs, ref byte[] values)
	{
		values = WriteSyncData(CreateReadHeader(id, unit, startAddress, numInputs, 2), id);
	}

	/// <summary>Read holding registers from slave asynchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	public void ReadHoldingRegister(ushort id, byte unit, ushort startAddress, ushort numInputs)
	{
		WriteAsyncData(CreateReadHeader(id, unit, startAddress, numInputs, 3), id);
	}

	/// <summary>Read holding registers from slave synchronous.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	/// <param name="values">Contains the result of function.</param>
	public void ReadHoldingRegister(ushort id, byte unit, ushort startAddress, ushort numInputs, ref byte[] values)
	{
		values = WriteSyncData(CreateReadHeader(id, unit, startAddress, numInputs, 3), id);
	}

	/// <summary>Read input registers from slave asynchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	public void ReadInputRegister(ushort id, byte unit, ushort startAddress, ushort numInputs)
	{
		WriteAsyncData(CreateReadHeader(id, unit, startAddress, numInputs, 4), id);
	}

	/// <summary>Read input registers from slave synchronous.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	/// <param name="values">Contains the result of function.</param>
	public void ReadInputRegister(ushort id, byte unit, ushort startAddress, ushort numInputs, ref byte[] values)
	{
		values = WriteSyncData(CreateReadHeader(id, unit, startAddress, numInputs, 4), id);
	}

	/// <summary>Write single coil in slave asynchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="OnOff">Specifys if the coil should be switched on or off.</param>
	public void WriteSingleCoils(ushort id, byte unit, ushort startAddress, bool OnOff)
	{
		byte[] array = CreateWriteHeader(id, unit, startAddress, 1, 1, 5);
		if (OnOff)
		{
			array[10] = byte.MaxValue;
		}
		else
		{
			array[10] = 0;
		}
		WriteAsyncData(array, id);
	}

	/// <summary>Write single coil in slave synchronous.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="OnOff">Specifys if the coil should be switched on or off.</param>
	/// <param name="result">Contains the result of the synchronous write.</param>
	public void WriteSingleCoils(ushort id, byte unit, ushort startAddress, bool OnOff, ref byte[] result)
	{
		byte[] array = CreateWriteHeader(id, unit, startAddress, 1, 1, 5);
		if (OnOff)
		{
			array[10] = byte.MaxValue;
		}
		else
		{
			array[10] = 0;
		}
		result = WriteSyncData(array, id);
	}

	/// <summary>Write multiple coils in slave asynchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numBits">Specifys number of bits.</param>
	/// <param name="values">Contains the bit information in byte format.</param>
	public void WriteMultipleCoils(ushort id, byte unit, ushort startAddress, ushort numBits, byte[] values)
	{
		byte b = Convert.ToByte(values.Length);
		byte[] array = CreateWriteHeader(id, unit, startAddress, numBits, (byte)(b + 2), 15);
		Array.Copy(values, 0, array, 13, b);
		WriteAsyncData(array, id);
	}

	/// <summary>Write multiple coils in slave synchronous.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address from where the data read begins.</param>
	/// <param name="numBits">Specifys number of bits.</param>
	/// <param name="values">Contains the bit information in byte format.</param>
	/// <param name="result">Contains the result of the synchronous write.</param>
	public void WriteMultipleCoils(ushort id, byte unit, ushort startAddress, ushort numBits, byte[] values, ref byte[] result)
	{
		byte b = Convert.ToByte(values.Length);
		byte[] array = CreateWriteHeader(id, unit, startAddress, numBits, (byte)(b + 2), 15);
		Array.Copy(values, 0, array, 13, b);
		result = WriteSyncData(array, id);
	}

	/// <summary>Write single register in slave asynchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address to where the data is written.</param>
	/// <param name="values">Contains the register information.</param>
	public void WriteSingleRegister(ushort id, byte unit, ushort startAddress, byte[] values)
	{
		byte[] array = CreateWriteHeader(id, unit, startAddress, 1, 1, 6);
		array[10] = values[0];
		array[11] = values[1];
		WriteAsyncData(array, id);
	}

	/// <summary>Write single register in slave synchronous.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address to where the data is written.</param>
	/// <param name="values">Contains the register information.</param>
	/// <param name="result">Contains the result of the synchronous write.</param>
	public void WriteSingleRegister(ushort id, byte unit, ushort startAddress, byte[] values, ref byte[] result)
	{
		byte[] array = CreateWriteHeader(id, unit, startAddress, 1, 1, 6);
		array[10] = values[0];
		array[11] = values[1];
		result = WriteSyncData(array, id);
	}

	/// <summary>Write multiple registers in slave asynchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address to where the data is written.</param>
	/// <param name="values">Contains the register information.</param>
	public void WriteMultipleRegister(ushort id, byte unit, ushort startAddress, byte[] values)
	{
		ushort num = Convert.ToUInt16(values.Length);
		if (num % 2 > 0)
		{
			num++;
		}
		byte[] array = CreateWriteHeader(id, unit, startAddress, Convert.ToUInt16(num / 2), Convert.ToUInt16(num + 2), 16);
		Array.Copy(values, 0, array, 13, values.Length);
		WriteAsyncData(array, id);
	}

	/// <summary>Write multiple registers in slave synchronous.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startAddress">Address to where the data is written.</param>
	/// <param name="values">Contains the register information.</param>
	/// <param name="result">Contains the result of the synchronous write.</param>
	public void WriteMultipleRegister(ushort id, byte unit, ushort startAddress, byte[] values, ref byte[] result)
	{
		ushort num = Convert.ToUInt16(values.Length);
		if (num % 2 > 0)
		{
			num++;
		}
		byte[] array = CreateWriteHeader(id, unit, startAddress, Convert.ToUInt16(num / 2), Convert.ToUInt16(num + 2), 16);
		Array.Copy(values, 0, array, 13, values.Length);
		result = WriteSyncData(array, id);
	}

	/// <summary>Read/Write multiple registers in slave asynchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startReadAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	/// <param name="startWriteAddress">Address to where the data is written.</param>
	/// <param name="values">Contains the register information.</param>
	public void ReadWriteMultipleRegister(ushort id, byte unit, ushort startReadAddress, ushort numInputs, ushort startWriteAddress, byte[] values)
	{
		ushort num = Convert.ToUInt16(values.Length);
		if (num % 2 > 0)
		{
			num++;
		}
		byte[] array = CreateReadWriteHeader(id, unit, startReadAddress, numInputs, startWriteAddress, Convert.ToUInt16(num / 2));
		Array.Copy(values, 0, array, 17, values.Length);
		WriteAsyncData(array, id);
	}

	/// <summary>Read/Write multiple registers in slave synchronous. The result is given in the response function.</summary>
	/// <param name="id">Unique id that marks the transaction. In asynchonous mode this id is given to the callback function.</param>
	/// <param name="unit">Unit identifier (previously slave address). In asynchonous mode this unit is given to the callback function.</param>
	/// <param name="startReadAddress">Address from where the data read begins.</param>
	/// <param name="numInputs">Length of data.</param>
	/// <param name="startWriteAddress">Address to where the data is written.</param>
	/// <param name="values">Contains the register information.</param>
	/// <param name="result">Contains the result of the synchronous command.</param>
	public void ReadWriteMultipleRegister(ushort id, byte unit, ushort startReadAddress, ushort numInputs, ushort startWriteAddress, byte[] values, ref byte[] result)
	{
		ushort num = Convert.ToUInt16(values.Length);
		if (num % 2 > 0)
		{
			num++;
		}
		byte[] array = CreateReadWriteHeader(id, unit, startReadAddress, numInputs, startWriteAddress, Convert.ToUInt16(num / 2));
		Array.Copy(values, 0, array, 17, values.Length);
		result = WriteSyncData(array, id);
	}

	private byte[] CreateReadHeader(ushort id, byte unit, ushort startAddress, ushort length, byte function)
	{
		byte[] array = new byte[12];
		byte[] bytes = BitConverter.GetBytes((short)id);
		array[0] = bytes[1];
		array[1] = bytes[0];
		array[5] = 6;
		array[6] = unit;
		array[7] = function;
		byte[] bytes2 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)startAddress));
		array[8] = bytes2[0];
		array[9] = bytes2[1];
		byte[] bytes3 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)length));
		array[10] = bytes3[0];
		array[11] = bytes3[1];
		return array;
	}

	private byte[] CreateWriteHeader(ushort id, byte unit, ushort startAddress, ushort numData, ushort numBytes, byte function)
	{
		byte[] array = new byte[numBytes + 11];
		byte[] bytes = BitConverter.GetBytes((short)id);
		array[0] = bytes[1];
		array[1] = bytes[0];
		byte[] bytes2 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)(5 + numBytes)));
		array[4] = bytes2[0];
		array[5] = bytes2[1];
		array[6] = unit;
		array[7] = function;
		byte[] bytes3 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)startAddress));
		array[8] = bytes3[0];
		array[9] = bytes3[1];
		if (function >= 15)
		{
			byte[] bytes4 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)numData));
			array[10] = bytes4[0];
			array[11] = bytes4[1];
			array[12] = (byte)(numBytes - 2);
		}
		return array;
	}

	private byte[] CreateReadWriteHeader(ushort id, byte unit, ushort startReadAddress, ushort numRead, ushort startWriteAddress, ushort numWrite)
	{
		byte[] array = new byte[numWrite * 2 + 17];
		byte[] bytes = BitConverter.GetBytes((short)id);
		array[0] = bytes[1];
		array[1] = bytes[0];
		byte[] bytes2 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)(11 + numWrite * 2)));
		array[4] = bytes2[0];
		array[5] = bytes2[1];
		array[6] = unit;
		array[7] = 23;
		byte[] bytes3 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)startReadAddress));
		array[8] = bytes3[0];
		array[9] = bytes3[1];
		byte[] bytes4 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)numRead));
		array[10] = bytes4[0];
		array[11] = bytes4[1];
		byte[] bytes5 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)startWriteAddress));
		array[12] = bytes5[0];
		array[13] = bytes5[1];
		byte[] bytes6 = BitConverter.GetBytes(IPAddress.HostToNetworkOrder((short)numWrite));
		array[14] = bytes6[0];
		array[15] = bytes6[1];
		array[16] = (byte)(numWrite * 2);
		return array;
	}

	private void WriteAsyncData(byte[] write_data, ushort id)
	{
		if (tcpAsyCl != null && tcpAsyCl.Connected)
		{
			try
			{
				tcpAsyCl.BeginSend(write_data, 0, write_data.Length, SocketFlags.None, OnSend, null);
				tcpAsyCl.BeginReceive(tcpAsyClBuffer, 0, tcpAsyClBuffer.Length, SocketFlags.None, OnReceive, tcpAsyCl);
				return;
			}
			catch (SystemException)
			{
				CallException(id, write_data[6], write_data[7], 254);
				return;
			}
		}
		CallException(id, write_data[6], write_data[7], 254);
	}

	private void OnSend(IAsyncResult result)
	{
		if (!result.IsCompleted)
		{
			CallException(ushort.MaxValue, byte.MaxValue, byte.MaxValue, 100);
		}
	}

	private void OnReceive(IAsyncResult result)
	{
		if (!result.IsCompleted)
		{
			CallException(255, byte.MaxValue, byte.MaxValue, 254);
		}
		ushort id = SwapUInt16(BitConverter.ToUInt16(tcpAsyClBuffer, 0));
		byte unit = tcpAsyClBuffer[6];
		byte b = tcpAsyClBuffer[7];
		byte[] array;
		if (b >= 5 && b != 23)
		{
			array = new byte[2];
			Array.Copy(tcpAsyClBuffer, 10, array, 0, 2);
		}
		else
		{
			array = new byte[tcpAsyClBuffer[8]];
			Array.Copy(tcpAsyClBuffer, 9, array, 0, tcpAsyClBuffer[8]);
		}
		if (b > 128)
		{
			b -= 128;
			CallException(id, unit, b, tcpAsyClBuffer[8]);
		}
		else if (this.OnResponseData != null)
		{
			this.OnResponseData(id, unit, b, array);
		}
	}

	private byte[] WriteSyncData(byte[] write_data, ushort id)
	{
		if (tcpSynCl.Connected)
		{
			try
			{
				tcpSynCl.Send(write_data, 0, write_data.Length, SocketFlags.None);
				int num = tcpSynCl.Receive(tcpSynClBuffer, 0, tcpSynClBuffer.Length, SocketFlags.None);
				byte unit = tcpSynClBuffer[6];
				byte b = tcpSynClBuffer[7];
				if (num == 0)
				{
					CallException(id, unit, write_data[7], 254);
				}
				if (b > 128)
				{
					b -= 128;
					CallException(id, unit, b, tcpSynClBuffer[8]);
					return null;
				}
				byte[] array;
				if (b >= 5 && b != 23)
				{
					array = new byte[2];
					Array.Copy(tcpSynClBuffer, 10, array, 0, 2);
				}
				else
				{
					array = new byte[tcpSynClBuffer[8]];
					Array.Copy(tcpSynClBuffer, 9, array, 0, tcpSynClBuffer[8]);
				}
				return array;
			}
			catch (SystemException)
			{
				CallException(id, write_data[6], write_data[7], 254);
			}
		}
		else
		{
			CallException(id, write_data[6], write_data[7], 254);
		}
		return null;
	}
}
