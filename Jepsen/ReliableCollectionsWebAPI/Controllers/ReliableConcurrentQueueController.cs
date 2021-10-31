// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

namespace ReliableCollectionsWebAPI.Controllers
{
    using System.Collections.Generic;
    using System.Threading;
    using System.Threading.Tasks;
    using Microsoft.AspNetCore.Mvc;
    using Microsoft.ServiceFabric.Data;
    using Microsoft.ServiceFabric.Data.Collections;
    using System.Net.Http;
    using System;
    using System.Fabric;

    [Route("api/[controller]")]
    public class ReliableConcurrentQueueController:Controller
    {
        private readonly IReliableStateManager StateManager;

        public ReliableConcurrentQueueController(IReliableStateManager stateManager)
        {
            this.StateManager = stateManager;
        }


        // GET VoteData/name
        [HttpGet("")]
        public async Task<IActionResult> Get()
        {
            IReliableConcurrentQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableConcurrentQueue<String>>("myReliableConcurrentQueue");
            using (var txn = this.StateManager.CreateTransaction())
            {
                long returnvalue = queue.Count;

                List<KeyValuePair<string, long>> result = new List<KeyValuePair<string, long>>();
                result.Add(new KeyValuePair<string, long>("Queue lenght", returnvalue));
                return this.Json(result);


            }
        }


        [HttpPut]
        public async Task<IActionResult> Put()
        {
            List<KeyValuePair<string, string>> result = new List<KeyValuePair<string, string>>();
            try
            {
                IReliableConcurrentQueue<long> queue = await this.StateManager.GetOrAddAsync<IReliableConcurrentQueue<long>>("myReliableConcurrentQueue");

                String transactionquery;
                if (!String.IsNullOrEmpty(HttpContext.Request.Query["query"]))
                {
                    transactionquery = HttpContext.Request.Query["query"];
                }

                else
                {
                    return NoContent();
                }


                result.Add(new KeyValuePair<string, string>("query", transactionquery));


                dynamic operationlist;
                operationlist = Newtonsoft.Json.JsonConvert.DeserializeObject(transactionquery);


                using (ITransaction tx = this.StateManager.CreateTransaction())
                {
                    ConditionalValue<long> returnvalue;
                    Boolean v;
                    foreach (var item in operationlist.transaction)
                    {

                        if (item.operation.Value == "qp")
                        {
                            result.Add(new KeyValuePair<string, string>(item.operation.Value, "Failed"));
                        }

                        else if (item.operation.Value == "de")
                        {

                            await queue.EnqueueAsync(tx, (long)item.value);

                            result.Add(new KeyValuePair<string, string>("de", item.value.Value.ToString()));

                        }
                        else if (item.operation.Value == "qd")
                        {

                            returnvalue = await queue.TryDequeueAsync(tx);
                            if (returnvalue.HasValue)
                            {
                                result.Add(new KeyValuePair<string, string>("qd", returnvalue.Value.ToString()));

                            }
                            else
                            {
                                result.Add(new KeyValuePair<string, string>(item.operation.Value, "False"));
                            }
                        }
                        else if (item.operation.Value == "a")
                        {

                            tx.Abort();
                            result.Add(new KeyValuePair<string, string>(item.operation.Value, "Completed"));
                            return this.Json(result);
                        }
                        else
                        {
                            result.Add(new KeyValuePair<string, string>(item.operation.Value, "Failed"));

                        }

                    }

                    await tx.CommitAsync();
                }
                return this.Json(result);

            }
            catch (Exception e)
            {
                result.Add(new KeyValuePair<string, string>("Exception", e.ToString()));
                return this.Json(result);
            }

        }

        // PUT VoteData/name
        [HttpDelete("")]
        public async Task<IActionResult> get()
        {
            CancellationToken ct = new CancellationToken();

            IReliableConcurrentQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableConcurrentQueue<String>>("myReliableConcurrentQueue");

            using (var txn = this.StateManager.CreateTransaction())
            {
                ConditionalValue<string> returnvalue = await queue.TryDequeueAsync(txn, ct);

                await txn.CommitAsync();

                if (returnvalue.HasValue)
                {
                    List<KeyValuePair<string, long>> result = new List<KeyValuePair<string, long>>();
                    result.Add(new KeyValuePair<string, long>("TryDequeueAsync", long.Parse( returnvalue.Value)));
                    return this.Json(result);
                }
                else
                {
                    return new AcceptedResult();
                }
            }

        }

 
        // PUT VoteData/name/count
        [HttpPut("{value}")]
        public async Task<IActionResult> put(String value)
        {
            CancellationToken ct = new CancellationToken();

            IReliableConcurrentQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableConcurrentQueue<String>>("myReliableConcurrentQueue");

            using (var txn = this.StateManager.CreateTransaction())
            {
                await queue.EnqueueAsync(txn, value, ct);
                await txn.CommitAsync();


            }

            return new AcceptedResult();



        }


    }
}